/*
 * Copyright 2015-2022 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.dodo.apate.engine;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dodo.apate.generator.BoundQuery;
import com.dodo.apate.generator.apateRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据发生器
 *
 * @version : 2022-07-25
 * @author 
 */
@Slf4j
class ProducerWorker implements ShutdownHook, Runnable {

    private final String          threadName;
    private final apateEngine     engine;
    private final apateRepository repository;
    private final apateMonitor    monitor;
    private final EventQueue      eventQueue;
    //
    private final AtomicBoolean   running;
    private volatile Thread       workThread;

    ProducerWorker(String threadName, apateEngine engine, apateMonitor monitor, EventQueue eventQueue, apateRepository repository){
        this.threadName = threadName;
        this.engine = engine;
        this.repository = repository;
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

    @Override
    public void run() {
        this.workThread = Thread.currentThread();
        this.workThread.setName(this.threadName);
        this.monitor.producerStart(this.threadName, this.workThread);

        while (this.testContinue()) {
            try {
                List<BoundQuery> queries = this.repository.generator();
                while (this.testContinue()) {
                    if (!this.eventQueue.tryOffer(queries)) {
                        Thread.sleep(100); // prevent empty loop
                    } else {
                        break;
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
}
