package com.dodo.apate.realdb;

import java.util.ArrayList;
import java.util.List;
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

public class MySqlTest {

    @Test
    public void workloadTest() throws Exception {

        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setDbType(DsType.MySQL);
        apateConfig.setTransaction(false);

        //apateConfig.setPolicy("extreme");
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.addIgnoreError("Duplicate");
        apateConfig.addIgnoreError("Data truncation: Incorrect datetime value");
        apateConfig.addIgnoreError("Data truncation: Out of range value for column");
        apateConfig.setOpsRatio("INSERT#30");
        apateConfig.setWriteQps(128);

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsMySql();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);

        List<apateTable> tables = new ArrayList<>();
        //        tables.add(generator.addTable(null, "console", "alert_config_detail"));
        //        tables.add(generator.addTable(null, "console", "alert_event_log"));
        //        tables.add(generator.addTable(null, "console", "alert_receiver"));
        tables.add(generator.addTable(null, "astro", "order_history"));
//        tables.add(generator.addTable(null, "demo", "app_code"));
        //        tables.add(generator.addTable(null, "pika", "test_json"));
        //        
        //        tables.add(generator.addTable(null, "devtester", "1table"));
        //        tables.add(generator.addTable(null, "devtester", "alert_config_detail"));
        //        tables.add(generator.addTable(null, "devtester", "alert_event_log"));
        //        tables.add(generator.addTable(null, "devtester", "alert_receiver"));
        //        tables.add(generator.addTable(null, "mode_test", "td_ccwjq_2020"));
        //        tables.add(generator.addTable(null, "devtester", "pk_table_time"));
        //        tables.add(generator.addTable(null, "devtester", "column_default_datetime"));
        //        tables.add(generator.addTable(null, "devtester", "test_bit"));

        for (apateTable tab : tables) {
            tab.setInsertPolitic(SqlPolitic.FullCol);
            tab.apply();
        }

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(3, 32);

        // 监控信息
        long t = System.currentTimeMillis();
        while (!engine.isExitSignal()) {
            if ((t + 1000) < System.currentTimeMillis()) {
                t = System.currentTimeMillis();
                System.out.println(engine.getMonitor());
            }

            if (engine.getMonitor().getSucceedInsert() > 50000) {
                System.out.println(engine.getMonitor());
                engine.shutdown();
            }
        }
    }
}
