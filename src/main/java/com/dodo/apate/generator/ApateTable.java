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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dodo.apate.generator.action.DeleteAction;
import com.dodo.apate.generator.action.InsertAction;
import com.dodo.apate.generator.action.UpdateAction;
import com.dodo.apate.generator.loader.DataLoader;
import com.dodo.apate.generator.loader.DataLoaderFactory;
import com.dodo.apate.generator.loader.DefaultDataLoaderFactory;
import com.dodo.schema.SchemaFramework;
import com.dodo.schema.dialect.Dialect;

/**
 * 要生成数据的表基本信息和配置信息
 * @version : 2022-07-25
 * @author 
 */
public class apateTable {

    private final String                   catalog;
    private final String                   schema;
    private final String                   table;
    private final Map<String, apateColumn> columnMap;
    private final List<apateColumn>        columnList;
    private final apateFactory             apateFactory;
    //
    private SqlPolitic                     insertPolitic;
    private SqlPolitic                     updateSetPolitic;
    private SqlPolitic                     wherePolitic;
    private Action                         insertGenerator;
    private Action                         updateGenerator;
    private Action                         deleteGenerator;
    private boolean                        useQualifier;
    private boolean                        keyChanges;
    private boolean                        hasKey;

    apateTable(String catalog, String schema, String table, apateFactory apateFactory){
        this.catalog = catalog;
        this.schema = schema;
        this.table = table;
        this.columnMap = new LinkedHashMap<>();
        this.columnList = new ArrayList<>();
        this.apateFactory = apateFactory;
        this.insertPolitic = SqlPolitic.RandomCol;
        this.updateSetPolitic = SqlPolitic.RandomCol;
        this.wherePolitic = SqlPolitic.KeyCol;
        this.useQualifier = true;
        this.hasKey = false;
    }

    public String getCatalog() { return catalog; }

    public String getSchema() { return schema; }

    public String getTable() { return table; }

    public SqlPolitic getInsertPolitic() { return insertPolitic; }

    public void setInsertPolitic(SqlPolitic insertPolitic) { this.insertPolitic = insertPolitic; }

    public SqlPolitic getUpdateSetPolitic() { return updateSetPolitic; }

    public void setUpdateSetPolitic(SqlPolitic updateSetPolitic) { this.updateSetPolitic = updateSetPolitic; }

    public SqlPolitic getWherePolitic() { return wherePolitic; }

    public void setWherePolitic(SqlPolitic wherePolitic) { this.wherePolitic = wherePolitic; }

    public boolean isUseQualifier() { return useQualifier; }

    public void setUseQualifier(boolean useQualifier) { this.useQualifier = useQualifier; }

    public boolean isKeyChanges() { return keyChanges; }

    public void setKeyChanges(boolean keyChanges) { this.keyChanges = keyChanges; }

    /** 是否拥有主键 */
    public boolean hasKey() {
        return this.hasKey;
    }

    /** 添加一个列 */
    public void addColumn(apateColumn apateColumn) {
        this.columnMap.put(apateColumn.getColumn(), apateColumn);
        this.columnList.add(apateColumn);
        this.hasKey = this.hasKey | apateColumn.isKey();
    }

    /** 获取所有列 */
    public List<String> getColumns() { return this.columnList.stream().map(apateColumn::getColumn).collect(Collectors.toList()); }

    /** 查找某个列 */
    public apateColumn findColumn(String columnName) {
        return this.columnMap.get(columnName);
    }

    /** 应用最新配置，并且创建 IUD 生成器 */
    public void apply() throws SQLException {
        List<apateColumn> insertColumns = new ArrayList<>();
        List<apateColumn> updateSetColumns = new ArrayList<>();
        List<apateColumn> updateWhereColumns = new ArrayList<>();
        List<apateColumn> deleteWhereColumns = new ArrayList<>();

        for (apateColumn apateColumn : this.columnList) {
            if (apateColumn.isGenerator(UseFor.Insert)) {
                insertColumns.add(apateColumn);
            }
            if (apateColumn.isGenerator(UseFor.UpdateSet)) {
                if (!(apateColumn.isKey() && !this.keyChanges)) {
                    updateSetColumns.add(apateColumn);
                }
            }
            if (apateColumn.isGenerator(UseFor.UpdateWhere)) {
                updateWhereColumns.add(apateColumn);
            }
            if (apateColumn.isGenerator(UseFor.DeleteWhere)) {
                deleteWhereColumns.add(apateColumn);
            }
            apateColumn.applyConfig();
        }

        DataLoaderFactory dataLoaderFactory = this.apateFactory.getapateConfig().getDataLoaderFactory();
        dataLoaderFactory = dataLoaderFactory == null ? new DefaultDataLoaderFactory() : dataLoaderFactory;
        Dialect dialect = SchemaFramework.getDialect(this.apateFactory.getapateConfig().getDbType());

        DataLoader dataLoader = dataLoaderFactory.createDataLoader(this.apateFactory.getapateConfig(), this.apateFactory.newConnection());
        this.insertGenerator = new InsertAction(this, dialect, insertColumns);
        this.updateGenerator = new UpdateAction(this, dialect, updateSetColumns, updateWhereColumns, dataLoader);
        this.deleteGenerator = new DeleteAction(this, dialect, deleteWhereColumns, dataLoader);
    }

    /** 生成一批 insert，每批语句都是相同的语句模版 */
    protected List<BoundQuery> buildInsert(int batchSize) throws SQLException {
        return this.insertGenerator.generatorAction(batchSize);
    }

    /** 生成一批 update，每批语句都是相同的语句模版 */
    protected List<BoundQuery> buildUpdate(int batchSize) throws SQLException {
        return this.updateGenerator.generatorAction(batchSize);
    }

    /** 生成一批 delete，每批语句都是相同的语句模版 */
    protected List<BoundQuery> buildDelete(int batchSize) throws SQLException {
        return this.deleteGenerator.generatorAction(batchSize);
    }

    @Override
    public String toString() {
        return "{catalog='" + this.catalog + "', schema='" + this.schema + "', table='" + this.table + "'}";
    }
}
