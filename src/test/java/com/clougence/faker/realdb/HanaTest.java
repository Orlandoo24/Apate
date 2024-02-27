package com.dodo.apate.realdb;

import com.alibaba.druid.pool.DruidDataSource;
import com.dodo.apate.apateConfig;
import com.dodo.apate.engine.apateEngine;
import com.dodo.apate.generator.apateFactory;
import com.dodo.apate.generator.apateRepository;
import com.dodo.apate.generator.apateTable;
import com.dodo.apate.generator.SqlPolitic;
import com.dodo.apate.generator.loader.PrecociousDataLoaderFactory;
import com.dodo.schema.DsType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HanaTest {
    @Test
    public void workloadTest() throws Exception {
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setTransaction(false);
        apateConfig.setDbType(DsType.Hana);
        //apateConfig.setPolicy("extreme");
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.addIgnoreError("Duplicate");
        apateConfig.addIgnoreError("Data truncation: Incorrect datetime value");
        apateConfig.addIgnoreError("Data truncation: Out of range value for column");
        apateConfig.setOpsRatio("INSERT#35;UPDATE#60;DELETE#5");
        apateConfig.setWriteQps(3);

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsHana();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);

        List<apateTable> tables = new ArrayList<>();
        tables.add(generator.addTable(null, "SYSTEM", "TEST_COLUMN"));
        //        tables.add(generator.addTable("devtester", null, "1table"));

        for (apateTable tab : tables) {
            tab.setInsertPolitic(SqlPolitic.RandomCol);
            tab.apply();
        }

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(3, 3);

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

    @Test
    public void workloadTest1() throws Exception {
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setTransaction(false);
        apateConfig.setDbType(DsType.Hana);
        //apateConfig.setPolicy("extreme");
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.addIgnoreError("Duplicate");
        apateConfig.addIgnoreError("Data truncation: Incorrect datetime value");
        apateConfig.addIgnoreError("Data truncation: Out of range value for column");
        apateConfig.setOpsRatio("INSERT#35;UPDATE#60;DELETE#5");
        apateConfig.setWriteQps(3);

        // 生成器，配置表
        DruidDataSource dataDs = DsUtils.dsHana();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);

        List<apateTable> tables = new ArrayList<>();
        tables.add(generator.addTable(null, "SYSTEM", "TEST_COLUMN_1"));
        //        tables.add(generator.addTable("devtester", null, "1table"));

        for (apateTable tab : tables) {
            tab.setInsertPolitic(SqlPolitic.RandomCol);
            tab.apply();
        }

        // 生成数据
        apateEngine engine = new apateEngine(dataDs, generator);
        engine.start(3, 3);

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
