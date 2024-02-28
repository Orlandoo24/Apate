package com.dodo.apate.realdb;

import com.alibaba.druid.pool.DruidDataSource;
import com.dodo.apate.apateConfig;
import com.dodo.apate.engine.ApateEngine;
import com.dodo.apate.generator.apateFactory;
import com.dodo.apate.generator.apateRepository;
import com.dodo.apate.generator.apateTable;
import com.dodo.apate.generator.SqlPolitic;
import com.dodo.apate.generator.loader.PrecociousDataLoaderFactory;
import net.hasor.cobble.logging.LoggerFactory;
import org.junit.Test;

/**
 *
 * @date: 2023/7/12 20:25
 * @description:
 */

public class RedshiftTest {

    @Test
    public void workloadTest() throws Exception {
//        LoggerFactory.useStdOutLogger();
        // 全局配置
        apateConfig apateConfig = new apateConfig();
        apateConfig.setTransaction(false);
//        apateConfig.setPolicy("extreme");
        apateConfig.setMinBatchSizePerOps(50);
        apateConfig.setMaxBatchSizePerOps(100);
        apateConfig.setMinOpsCountPerTransaction(50);
        apateConfig.setMaxOpsCountPerTransaction(150);
        apateConfig.setDataLoaderFactory(new PrecociousDataLoaderFactory());
        apateConfig.setOpsRatio("I#100");
//        apateConfig.setCustomTpcConf("redshift-widely.tpc");

        DruidDataSource dataDs = com.dodo.apate.realdb.DsUtils.dsRedshift();
        apateFactory factory = new apateFactory(dataDs, apateConfig);
        apateRepository generator = new apateRepository(factory);

        apateTable table = generator.addTable("dev", "public", "tb_user");
        table.setInsertPolitic(SqlPolitic.FullCol);
        table.setUpdateSetPolitic(SqlPolitic.RandomCol);
        table.apply();

        // 生成数据
        ApateEngine engine = new ApateEngine(dataDs, generator);
        engine.start(40, 80);

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
