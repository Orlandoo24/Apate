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

public class SqlServerTest {

    @Test
    public void workloadTest() throws Exception {
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setDbType(DsType.SqlServer);
        apateConfig.setTransaction(false);
        //apateConfig.setPolicy("extreme");
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        //        apateConfig.addIgnoreError("Duplicate");
        //        apateConfig.addIgnoreError("restarting");
        //        apateConfig.addIgnoreError("deadlocked");
        //        apateConfig.addIgnoreError("was deadlocked on lock");
        apateConfig.addIgnoreError("违反了 PRIMARY KEY");
        apateConfig.addIgnoreError("违反了 UNIQUE KEY");
        apateConfig.addIgnoreError("The incoming tabular data stream (TDS) remote procedure call (RPC) protocol stream is incorrect");
        apateConfig.setOpsRatio("INSERT#50;UPDATE#50;DELETE#10");//");//UPDATE#50;DELETE#10");

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsSqlServer();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);
        generator.addTable("console", "dbo", "alert_config_detail");
        generator.addTable("console", "dbo", "alert_event_log");
        //        generator.addTable("console", "dbo", "alert_receiver");
        //        generator.addTable("console", "dbo", "mt5_deals");
        //        generator.addTable("console", "dbo", "mts_lang_data");
        //        generator.addTable("console", "dbo", "tb_sqlserver_types");
        //        generator.addTable("console", "dbo", "ver_table_1");
        //        generator.addTable("console", "dbo", "ver_table_2");
        //        generator.addTable("console", "dbo", "ver_table_3");

        //tb_sqlserver_types

        //        generator.scanTable("console", "dbo");
        //        table.setInsertPolitic(SqlPolitic.FullCol);
        //        table.apply();

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(1, 20);

        // 监控信息
        long t = System.currentTimeMillis();
        while (!engine.isExitSignal()) {
            if ((t + 1000) < System.currentTimeMillis()) {
                t = System.currentTimeMillis();
                System.out.println(engine.getMonitor());
            }

            if (engine.getMonitor().getSucceedInsert() > 100000) {
                System.out.println(engine.getMonitor());
                engine.shutdown();
            }
        }
    }
}
