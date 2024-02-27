/*
 * Copyright 2015-2022 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.dodo.apate.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dodo.apate.apateConfig;
import com.dodo.apate.generator.BoundQuery;
import com.dodo.apate.generator.SqlArg;

/**
 * 写入器
 *
 * @version : 2022-07-25
 * @author 
 */
class WriteWorker implements ShutdownHook, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(WriteWorker.class);
    private final String        threadName;
    private final apateEngine   engine;
    private final DataSource    dataSource;
    private final apateConfig   apateConfig;
    private final apateMonitor  monitor;
    private final EventQueue    eventQueue;
    //
    private final AtomicBoolean running;
    private volatile Thread     workThread;
    private List<String>        sqlTemp;

    public WriteWorker(String threadName, apateEngine engine, apateMonitor monitor, EventQueue eventQueue, DataSource dataSource, apateConfig apateConfig){
        this.threadName = threadName;
        this.engine = engine;
        this.dataSource = dataSource;
        this.apateConfig = apateConfig;
        this.monitor = monitor;
        this.eventQueue = eventQueue;
        this.running = new AtomicBoolean(true);
    }

    @Override
    public void shutdown() {
        if (this.running.compareAndSet(true, false)) {
            if (this.workThread != null) {
                this.workThread.interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() { return this.running.get(); }

    private boolean testContinue() {
        return this.running.get() && !this.engine.isExitSignal() && !Thread.interrupted();
    }

    private static String newBatchID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void run() {
        this.sqlTemp = this.apateConfig.isPrintSql() ? new ArrayList<>() : null;
        this.workThread = Thread.currentThread();
        this.workThread.setName(this.threadName);
        this.monitor.writerStart(this.threadName, this.workThread);

        while (this.testContinue()) {
            try {
                List<BoundQuery> queries = this.eventQueue.tryPoll();
                if (queries == null) {
                    Thread.sleep(100); // prevent empty loop
                    continue;
                }

                try (Connection conn = this.dataSource.getConnection()) {
                    try {
                        String batchID = null;
                        if (this.apateConfig.isTransaction()) {
                            Thread.sleep(this.apateConfig.randomPausePerTransactionMs());
                            batchID = newBatchID();
                            conn.setAutoCommit(false);
                        }

                        doBatch(batchID, conn, queries);

                        if (this.apateConfig.isTransaction()) {
                            conn.commit();
                        }
                    } finally {
                        if (this.apateConfig.isTransaction()) {
                            conn.setAutoCommit(true);
                        }
                    }
                }
            } catch (InterruptedException e) {
                this.running.set(false);
                return;
            } catch (Throwable e) {
                this.monitor.workThrowable(this.threadName, e);
            }
        }
    }

    private void doBatch(final String tranID, Connection conn, List<BoundQuery> batch) throws SQLException {
        for (BoundQuery event : batch) {
            if (!this.testContinue()) {
                return;
            }

            String useTranID = tranID;
            if (useTranID == null) {
                useTranID = newBatchID();
            }

            try {
                int affectRows = doEvent(conn, event);
                this.monitor.recordMonitor(this.threadName, useTranID, event, affectRows);
            } catch (SQLException e) {
                if (this.apateConfig.ignoreError(e)) {
                    this.monitor.recordIgnore(this.threadName, useTranID, event, e);
                } else {
                    this.monitor.recordFailed(this.threadName, useTranID, event, e);
                    logger.error(e.getMessage() + " event is " + event, e);
                    throw e;
                }
            }
        }
    }

    private int doEvent(Connection conn, BoundQuery event) throws SQLException {
        this.engine.checkQoS(); // 写入限流

        final String sqlString = event.getSqlString();
        final SqlArg[] sqlArgs = event.getArgs();

        if (this.sqlTemp != null && !this.sqlTemp.contains(sqlString)) {
            this.sqlTemp.add(sqlString);
            logger.info(sqlString);
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlString)) {
            if (this.apateConfig.getQueryTimeout() > 0) {
                ps.setQueryTimeout(this.apateConfig.getQueryTimeout());
            }

            for (int i = 1; i <= sqlArgs.length; i++) {
                if (sqlArgs[i - 1] == null) {
                    ps.setObject(i, null);
                } else {
                    sqlArgs[i - 1].setParameter(ps, i);
                }
            }

            return ps.executeUpdate();
        }
    }
}
