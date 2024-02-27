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

public class DmTest {

    @Test
    public void workloadTest() throws Exception {
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setDbType(DsType.Dameng);
        apateConfig.setTransaction(false);
        //        apateConfig.setPolicy("extreme");
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.addIgnoreError("唯一性约束");
        apateConfig.addIgnoreError("Data truncation: Incorrect datetime value");
        apateConfig.addIgnoreError("Data truncation: Out of range value for column");
        //apateConfig.setOpsRatio("INSERT#30");
        apateConfig.setWriteQps(10);

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsDm8();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);

        List<apateTable> tables = new ArrayList<>();
        tables.add(generator.addTable(null, "TESTER", "TB_DM_TYPES_FOR_V8"));
        //        tables.add(generator.addTable("devtester", null, "1table"));
        //        tables.add(generator.addTable("devtester", null, "alert_config_detail"));
        //        tables.add(generator.addTable("devtester", null, "alert_event_log"));
        //        tables.add(generator.addTable("devtester", null, "alert_receiver"));
        //        tables.add(generator.addTable("mode_test", null, "td_ccwjq_2020"));
        //        tables.add(generator.addTable("devtester", null, "pk_table_time"));
        //        tables.add(generator.addTable("devtester", null, "column_default_datetime"));
        //        tables.add(generator.addTable("devtester", null, "test_bit"));

        for (apateTable tab : tables) {
            tab.setInsertPolitic(SqlPolitic.FullCol);
            tab.apply();
        }

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(3, 10);

        // 监控信息
        long t = System.currentTimeMillis();
        while (!engine.isExitSignal()) {
            if ((t + 1000) < System.currentTimeMillis()) {
                t = System.currentTimeMillis();
                System.out.println(engine.getMonitor());
            }

            if (engine.getMonitor().getSucceedInsert() > 1000000) {
                System.out.println(engine.getMonitor());
                engine.shutdown();
            }
        }
    }
}
