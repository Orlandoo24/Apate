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

public class PostgresTest {

    @Test
    public void workloadTest() throws Exception {
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setDbType(DsType.PostgreSQL);
        apateConfig.setTransaction(false);
        //        apateConfig.setPolicy("extreme");
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.addIgnoreError("Duplicate");
        apateConfig.addIgnoreError("Data truncation: Incorrect datetime value");
        apateConfig.setWriteQps(10);
        //        apateConfig.setOpsRatio("I#10");//;U#30;D#10

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsPg();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);
        //
        //
        apateTable table1 = generator.addTable("pika", "public", "users");

        //        apateTable table1 = generator.addTable("postgres", "public", "pg_huasheng1");
        //        apateTable table2 = generator.addTable("devtester", "public", "alert_config_detail");
        //        apateTable table3 = generator.addTable("devtester", "public", "alert_event_log");
        //        apateTable table4 = generator.addTable("devtester", "public", "alert_receiver");
        //        apateTable table5 = generator.addTable("devtester", "public", "column_default_datetime");
        //        apateTable table6 = generator.addTable("devtester", "public", "tb_postgre_types");
        //        apateTable table7 = generator.addTable("devtester", "public", "test_bit");

        //        apateTable table1 = generator.addTable("postgres", "public", "tb_postgre_types");
        //        apateTable table2 = generator.addTable("postgres", "pipi_test", "test_uuid_bool");
        //        apateTable table3 = generator.addTable("pggis", "pggis", "td_ccwjq_2020");
        //        apateTable table4 = generator.addTable("pggis", "pggis", "td_cjdcq_2020");
        table1.setInsertPolitic(SqlPolitic.FullCol);
        table1.apply();

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(30, 10);

        // 监控信息
        long t = System.currentTimeMillis();
        while (!engine.isExitSignal()) {
            if ((t + 1000) < System.currentTimeMillis()) {
                t = System.currentTimeMillis();
                System.out.println(engine.getMonitor());
            }

            if (engine.getMonitor().getSucceedInsert() > 10000) {
                System.out.println(engine.getMonitor());
                engine.shutdown();
            }
        }
    }
}
