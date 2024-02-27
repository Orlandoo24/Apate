/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dodo.apate.generator;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dodo.apate.apateConfig;
import com.dodo.apate.OpsType;
import com.dodo.schema.dialect.DefaultDialect;
import com.dodo.utils.CollectionUtils;
import com.dodo.utils.RandomUtils;
import com.dodo.utils.StringUtils;

import net.hasor.cobble.setting.DefaultSettings;
import net.hasor.cobble.setting.SettingNode;
import net.hasor.cobble.setting.provider.StreamType;

/**
 * apateGenerator
 * @version : 2022-07-25
 * @author 
 */
public class apateRepository {

    private final static Logger    logger = LoggerFactory.getLogger(apateRepository.class);
    private final String           generatorID;
    private final apateConfig      apateConfig;
    private final apateFactory     apateFactory;
    private final List<apateTable> generatorTables;

    public apateRepository(apateFactory apateFactory){
        this.generatorID = UUID.randomUUID().toString().replace("-", "");
        this.apateConfig = apateFactory.getapateConfig();
        this.apateFactory = apateFactory;
        this.generatorTables = new CopyOnWriteArrayList<>();
    }

    public String getGeneratorID() { return this.generatorID; }

    public apateConfig getConfig() { return this.apateConfig; }

    /** 从生成器中随机选择一张 apateTable 表，并为这张表生成一个事务的语句。语句类型随机 */
    public List<BoundQuery> generator() throws SQLException {
        return generator(this.apateConfig.randomOps());
    }

    /** 从生成器中随机选择一张 apateTable 表，并为这张表生成一个事务的语句。语句类型由 opsType 决定 */
    public List<BoundQuery> generator(OpsType opsType) throws SQLException {
        apateTable table = randomTable();
        if (table == null) {
            return Collections.emptyList();
        }

        List<BoundQuery> events = new LinkedList<>();
        int opsCountPerTransaction = this.apateConfig.randomOpsCountPerTrans();
        for (int i = 0; i < opsCountPerTransaction; i++) {
            List<BoundQuery> dataSet = this.generatorOps(table, opsType);
            events.addAll(dataSet);
        }
        return events;
    }

    /** 从生成器中随机选择一张 apateTable 表 */
    protected apateTable randomTable() {
        if (!CollectionUtils.isEmpty(this.generatorTables)) {
            if (this.generatorTables.size() == 1) {
                return this.generatorTables.get(0);
            } else {
                return this.generatorTables.get(RandomUtils.nextInt(0, this.generatorTables.size()));
            }
        }
        return null;
    }

    /** 为 apateTable 生成一批 opsType 类型 DML 语句 */
    protected List<BoundQuery> generatorOps(apateTable apateTable, OpsType opsType) throws SQLException {
        Objects.requireNonNull(apateTable, "apateTable is null.");
        Objects.requireNonNull(opsType, "opsType is null.");

        int batchSize = this.apateConfig.randomBatchSizePerOps();
        switch (opsType) {
            case Insert:
                return apateTable.buildInsert(batchSize);
            case Update:
                return apateTable.buildUpdate(batchSize);
            case Delete:
                return apateTable.buildDelete(batchSize);
            default:
                return Collections.emptyList();
        }
    }

    /** 添加一个表到生成器中，表的列信息通过元信息服务来补全。 */
    public apateTable addTable(String catalog, String schema, String table) throws SQLException {
        try {
            apateTable fetchTable = this.apateFactory.fetchTable(catalog, schema, table);
            this.addTable(fetchTable);
            return fetchTable;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("init table failed : " + e.getMessage(), e);
        }
    }

    /** 添加一个表到生成器中 */
    protected apateTable addTable(apateTable table) {
        this.generatorTables.add(table);
        return table;
    }

    /** 从生成器中查找某个表 */
    public apateTable findTable(String catalog, String schema, String table) {
        return this.generatorTables.stream().filter(apateTable -> {
            return StringUtils.equals(apateTable.getCatalog(), catalog) && //
            StringUtils.equals(apateTable.getSchema(), schema) && //
            StringUtils.equals(apateTable.getTable(), table);
        }).findFirst().orElse(null);
    }

    public void loadConfig(String config, StreamType streamType) throws Exception {
        DefaultSettings settings = new DefaultSettings();
        settings.loadResource(config, streamType);

        SettingNode[] tables = settings.getNodeArray("config.table");
        if (tables != null) {
            for (SettingNode table : tables) {
                apateTable apateTable = this.apateFactory.fetchTable(table);
                if (apateTable != null) {
                    String tableName = DefaultDialect.DEFAULT.fmtTableName(true, apateTable.getCatalog(), apateTable.getSchema(), apateTable.getTable());
                    this.addTable(apateTable);
                    logger.info("found table '" + tableName + "'");
                }
            }
        }
    }
}
