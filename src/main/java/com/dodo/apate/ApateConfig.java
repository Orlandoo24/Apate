/*
 * Copyright 2015-2022 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.dodo.apate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import org.apache.ibatis.type.TypeHandlerRegistry;

import com.dodo.apate.generator.loader.DataLoaderFactory;
import com.dodo.apate.generator.processor.TypeProcessorFactory;
import com.dodo.apate.utils.RandomRatio;
import com.dodo.apate.utils.RatioUtils;
import com.dodo.apate.utils.types.TypeHandlerRegistryUtils;
import com.dodo.schema.DsType;
import com.dodo.schema.umi.service.rdb.RdbUmiService;
import com.dodo.utils.RandomUtils;
import com.dodo.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * apate 全局配置
 *
 * @version : 2022-07-25
 * @author 
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class apateConfig {

    // 生成器
    private ClassLoader                classLoader;                 // 类加载器
    private TypeHandlerRegistry        typeRegistry;                // 类型处理注册表
    private DataLoaderFactory          dataLoaderFactory;           // 数据加载器工厂
    private RdbUmiService              customFetchMeta;             // 自定义抓取元数据
    private TypeProcessorFactory       typeProcessorFactory;        // 类型处理器工厂
    private String                     policy;                      // 策略
    private DsType                     dbType;                      // 数据库类型
    private String                     customTpcConf;               // 自定义 TPC 配置
    private boolean                    useQualifier;                // 使用限定符
    private RandomMode                 randomMode;                  // 随机模式
    private boolean                    keyChanges;                  // 键变化
    private boolean                    printSql;                    // 打印 SQL

    // 单事务
    private int                        minBatchSizePerOps;          // 每个操作的最小批量大小
    private int                        maxBatchSizePerOps;          // 每个操作的最大批量大小
    private final RandomRatio<OpsType> opsRatio;                   // 操作比率
    private int                        minOpsCountPerTransaction;   // 每个事务的最小操作数量
    private int                        maxOpsCountPerTransaction;   // 每个事务的最大操作数量

    // 事务流
    private boolean                    transaction;                 // 是否事务
    private int                        minPausePerTransactionMs;    // 每个事务的最小暂停时间（毫秒）
    private int                        maxPausePerTransactionMs;    // 每个事务的最大暂停时间（毫秒）

    // 工作线程
    private ThreadFactory              threadFactory;               // 线程工厂
    private int                        queueCapacity;               // 队列容量
    private int                        writeQps;                    // 写入 QPS
    private int                        queryTimeout;                // 查询超时
    private final Set<String>          ignoreErrors;                // 忽略的错误集合
    private boolean                    ignoreAnyErrors;             // 是否忽略所有错误

    @Tolerate
    public apateConfig(){
        // 默认值设置
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.typeRegistry = TypeHandlerRegistryUtils.DEFAULT;
        this.dataLoaderFactory = null;
        this.typeProcessorFactory = null;
        this.useQualifier = true;
        this.randomMode = RandomMode.RandomQuery;
        //
        this.minBatchSizePerOps = 2;
        this.maxBatchSizePerOps = 5;
        this.opsRatio = RatioUtils.passerByConfig("INSERT#30;UPDATE#30;DELETE#30");
        this.minOpsCountPerTransaction = 5;
        this.maxOpsCountPerTransaction = 10;
        //
        this.transaction = true;
        this.minPausePerTransactionMs = 0;
        this.maxPausePerTransactionMs = 0;
        //
        this.queueCapacity = 4096;
        this.writeQps = -1;
        this.queryTimeout = -1;
        this.ignoreErrors = new HashSet<>(Arrays.asList("Duplicate", "duplicate"));
        this.ignoreAnyErrors = false;
    }

    public int randomOpsCountPerTrans() {
        return RandomUtils.nextInt(Math.min(1, this.minOpsCountPerTransaction), Math.max(1, this.maxOpsCountPerTransaction));
    }

    public int randomPausePerTransactionMs() {
        return RandomUtils.nextInt(Math.min(1, this.minPausePerTransactionMs), Math.max(1, this.maxPausePerTransactionMs));
    }

    public int randomBatchSizePerOps() {
        return RandomUtils.nextInt(Math.min(1, this.minBatchSizePerOps), Math.max(1, this.maxBatchSizePerOps));
    }

    public OpsType randomOps() {
        return this.opsRatio.getByRandom();
    }

    public boolean ignoreError(Exception e) {
        if (this.ignoreAnyErrors) {
            return true;
        }

        if (this.ignoreErrors.isEmpty()) {
            return false;
        }

        for (String term : this.ignoreErrors) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), term)) {
                return true;
            }
        }
        return false;
    }

    public void addIgnoreError(String keyWords) {
        if (StringUtils.isNotBlank(keyWords)) {
            this.ignoreErrors.add(keyWords);
        }
    }

    public void setOpsRatio(String opsRatio) {
        this.opsRatio.clearRatio();
        RatioUtils.fillByConfig(opsRatio, this.opsRatio);
    }
}

