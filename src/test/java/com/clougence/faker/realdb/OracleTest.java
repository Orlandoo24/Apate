package com.dodo.apate.realdb;

import org.junit.Test;

import com.alibaba.druid.pool.DruidDataSource;
import com.dodo.apate.apateConfig;
import com.dodo.apate.engine.apateEngine;
import com.dodo.apate.generator.apateFactory;
import com.dodo.apate.generator.apateRepository;
import com.dodo.apate.generator.apateTable;
import com.dodo.apate.generator.SqlPolitic;
import com.dodo.apate.generator.loader.PrecociousDataLoaderFactory;
import com.dodo.schema.DsType;

public class OracleTest {

    @Test
    public void workloadTest() throws Exception {
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setDbType(DsType.Oracle);
        apateConfig.setTransaction(true);
//        apateConfig.setPolicy("extreme");
        apateConfig.setMinBatchSizePerOps(5);
        apateConfig.setMaxBatchSizePerOps(10);
        apateConfig.setMinOpsCountPerTransaction(5);
        apateConfig.setMaxOpsCountPerTransaction(15);
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.addIgnoreError("ORA-00001");
//        apateConfig.addIgnoreError("ORA-01438");
//        apateConfig.setOpsRatio("I#10;U#10");
        apateConfig.setCustomTpcConf("oracle-varcharmax.tpc");
        //        apateConfig.addIgnoreError("restarting");
        //        apateConfig.addIgnoreError("deadlocked");
        //        apateConfig.addIgnoreError("was deadlocked on lock");
        //        apateConfig.setOpsRatio("D#30");

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsOracle();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);
        // apateTable table = generator.addTable("console", "dbo", "tb_sqlserver_types");
        // apateTable table = generator.addTable("console", "dbo", "stock");
//        apateTable table = generator.addTable(null, "SCOTT", "TB_ORACLE_TYPES");
//        apateTable table = generator.addTable("ORCL", "JUNYU_ORCL", "WORKER_STATS");
        apateTable table2 = generator.addTable(null, "FANQIE", "A");
        table2.setInsertPolitic(SqlPolitic.FullCol);
        table2.setUpdateSetPolitic(SqlPolitic.RandomCol);
        table2.apply();
        apateTable table3 = generator.addTable(null, "FANQIE", "A1");
        table3.setInsertPolitic(SqlPolitic.FullCol);
        table3.setUpdateSetPolitic(SqlPolitic.RandomCol);
        table3.apply();
        apateTable table4 = generator.addTable(null, "FANQIE", "B");
        table4.setInsertPolitic(SqlPolitic.FullCol);
        table4.setUpdateSetPolitic(SqlPolitic.RandomCol);
        table4.apply();
        apateTable table5 = generator.addTable(null, "FANQIE", "DEMO");
        table5.setInsertPolitic(SqlPolitic.FullCol);
        table5.setUpdateSetPolitic(SqlPolitic.RandomCol);
        table5.apply();
//        table3.setInsertPolitic(SqlPolitic.FullCol);
//        table3.setUpdateSetPolitic(SqlPolitic.RandomCol);
//        table3.apply();

//        table2.setInsertPolitic(SqlPolitic.FullCol);
//        table2.setUpdateSetPolitic(SqlPolitic.RandomCol);
//        table2.apply();

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(8, 16);

        // 监控信息
        long t = System.currentTimeMillis();
        while (!engine.isExitSignal()) {
            if ((t + 1000) < System.currentTimeMillis()) {
                t = System.currentTimeMillis();
                System.out.println(engine.getMonitor());
            }

            if (engine.getMonitor().getSucceedInsert() > 200000) {
                System.out.println(engine.getMonitor());
                engine.shutdown();
            }
        }
    }
}
