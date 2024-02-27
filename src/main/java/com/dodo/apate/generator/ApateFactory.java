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

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dodo.apate.apateConfig;
import com.dodo.apate.apateConfigEnum;
import com.dodo.apate.generator.processor.DslTypeProcessorFactory;
import com.dodo.apate.generator.processor.TypeProcessorFactory;
import com.dodo.apate.seed.SeedConfig;
import com.dodo.apate.seed.SeedFactory;
import com.dodo.apate.seed.SeedType;
import com.dodo.apate.seed.array.ArraySeedFactory;
import com.dodo.apate.utils.UmiUtils;
import com.dodo.schema.umi.service.rdb.RdbUmiService;
import com.dodo.schema.umi.special.rdb.RdbColumn;
import com.dodo.schema.umi.special.rdb.RdbTable;
import com.dodo.schema.umi.struts.constraint.GeneralConstraintType;
import com.dodo.utils.BeanUtils;
import com.dodo.utils.StringUtils;
import com.dodo.utils.function.ESupplier;

import net.hasor.cobble.setting.SettingNode;
import net.hasor.cobble.setting.data.TreeNode;

/**
 * apateTable 构建器
 * @version : 2022-07-25
 * @author 
 */
public class apateFactory {

    private static final Logger                       logger = LoggerFactory.getLogger(apateFactory.class);
    private final ESupplier<Connection, SQLException> connection;
    private final RdbUmiService                       umiService;
    private final apateConfig                         apateConfig;
    private final Map<String, Object>                 variables;
    private final TypeProcessorFactory                typeDialect;

    public apateFactory(Connection connection) throws SQLException, IOException{
        this(() -> newProxyConnection(connection), new apateConfig());
    }

    public apateFactory(DataSource dataSource) throws SQLException, IOException{
        this(dataSource::getConnection, new apateConfig());
    }

    public apateFactory(Connection connection, apateConfig config) throws SQLException, IOException{
        this(() -> newProxyConnection(connection), config);
    }

    public apateFactory(DataSource dataSource, apateConfig config) throws SQLException, IOException{
        this(dataSource::getConnection, config);
    }

    protected apateFactory(ESupplier<Connection, SQLException> connection, apateConfig config) throws SQLException, IOException{
        this.connection = connection;
        this.apateConfig = config;
        this.umiService = UmiUtils.newUmiService(connection, config);
        this.variables = this.initVariables(config);
        this.typeDialect = this.initTypeDialect(config, this.variables);
    }

    protected Map<String, Object> initVariables(apateConfig apateConfig) throws SQLException {
        Map<String, Object> javaVars = new HashMap<>();
        System.getProperties().forEach((k, v) -> {
            javaVars.put(String.valueOf(k), v);
        });

        Map<String, Object> envVars = new HashMap<>();
        System.getenv().forEach((k, v) -> envVars.put(String.valueOf(k), v));

        Map<String, Object> globalVars = new HashMap<>();
        globalVars.put("java", javaVars);
        globalVars.put("env", envVars);
        globalVars.put("dbType", apateConfig.getDbType().getTypeName());
        globalVars.put("policy", apateConfig.getPolicy());

        try (Connection conn = this.connection.eGet()) {
            DatabaseMetaData metaData = conn.getMetaData();
            globalVars.put("jdbcUrl", metaData.getURL());
            globalVars.put("driverName", metaData.getDriverName());
            globalVars.put("driverVersion", metaData.getDriverVersion());
            globalVars.put("dbMajorVersion", metaData.getDatabaseMajorVersion());
            globalVars.put("dbMinorVersion", metaData.getDatabaseMinorVersion());
            globalVars.put("dbProductName", metaData.getDatabaseProductName());
            globalVars.put("dbProductVersion", metaData.getDatabaseProductVersion());
            return globalVars;
        }
    }

    public Connection newConnection() throws SQLException {
        return this.connection.eGet();
    }

    protected TypeProcessorFactory initTypeDialect(apateConfig apateConfig, Map<String, Object> variables) throws IOException {
        if (apateConfig.getTypeProcessorFactory() != null) {
            return apateConfig.getTypeProcessorFactory();
        }

        return new DslTypeProcessorFactory(apateConfig.getDbType(), variables, apateConfig);
    }

    protected apateConfig getapateConfig() { return this.apateConfig; }

    public apateTable fetchTable(SettingNode tableConfig) throws SQLException, ReflectiveOperationException {
        String catalog = tableConfig.getSubValue(apateConfigEnum.TABLE_CATALOG.getConfigKey());
        String schema = tableConfig.getSubValue(apateConfigEnum.TABLE_SCHEMA.getConfigKey());
        String table = tableConfig.getSubValue(apateConfigEnum.TABLE_TABLE.getConfigKey());

        return this.buildTable(catalog, schema, table, tableConfig);
    }

    public apateTable fetchTable(String catalog, String schema, String table) throws SQLException, ReflectiveOperationException {
        return this.buildTable(catalog, schema, table, null);
    }

    public apateTable buildTable(String catalog, String schema, String table, SettingNode tableConfig) throws SQLException, ReflectiveOperationException {
        RdbTable rdbTable = this.umiService.loadTable(null, catalog, schema, table);
        if (rdbTable == null) {
            String tabName = String.format("%s.%s.%s", catalog, schema, table);
            throw new IllegalArgumentException("table '" + tabName + "' is not exist.");
        }

        apateTable apateTable = new apateTable(catalog, schema, table, this);
        apateTable.setUseQualifier(this.apateConfig.isUseQualifier());
        apateTable.setKeyChanges(this.apateConfig.isKeyChanges());

        tableConfig = tableConfig == null ? new TreeNode() : tableConfig;
        buildColumns(apateTable, tableConfig, rdbTable);

        String insertPoliticStr = tableConfig.getSubValue(apateConfigEnum.TABLE_ACT_POLITIC_INSERT.getConfigKey());
        String updatePoliticStr = tableConfig.getSubValue(apateConfigEnum.TABLE_ACT_POLITIC_UPDATE.getConfigKey());
        String wherePoliticStr = tableConfig.getSubValue(apateConfigEnum.TABLE_ACT_POLITIC_WHERE.getConfigKey());

        apateTable.setInsertPolitic(SqlPolitic.valueOfCode(insertPoliticStr, SqlPolitic.RandomCol));
        apateTable.setUpdateSetPolitic(SqlPolitic.valueOfCode(updatePoliticStr, SqlPolitic.RandomCol));
        apateTable.setWherePolitic(SqlPolitic.valueOfCode(wherePoliticStr, SqlPolitic.KeyCol));

        apateTable.apply();
        return apateTable;
    }

    protected void buildColumns(apateTable apateTable, SettingNode tableConfig, RdbTable refTable) throws ReflectiveOperationException {
        SettingNode columnsConfig = tableConfig.getSubNode(apateConfigEnum.TABLE_COLUMNS.getConfigKey());
        String[] ignoreCols = tableConfig.getSubValues(apateConfigEnum.TABLE_COL_IGNORE_ALL.getConfigKey());
        String[] ignoreInsertCols = tableConfig.getSubValues(apateConfigEnum.TABLE_COL_IGNORE_INSERT.getConfigKey());
        String[] ignoreUpdateCols = tableConfig.getSubValues(apateConfigEnum.TABLE_COL_IGNORE_UPDATE.getConfigKey());
        String[] ignoreWhereCols = tableConfig.getSubValues(apateConfigEnum.TABLE_COL_IGNORE_WHERE.getConfigKey());
        Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreCols));
        Set<String> ignoreInsertSet = new HashSet<>(Arrays.asList(ignoreInsertCols));
        Set<String> ignoreUpdateSet = new HashSet<>(Arrays.asList(ignoreUpdateCols));
        Set<String> ignoreWhereSet = new HashSet<>(Arrays.asList(ignoreWhereCols));

        Collection<RdbColumn> columns = refTable.getColumns().values();
        if (columns.isEmpty()) {
            throw new UnsupportedOperationException(apateTable + " no columns were found in the meta information.");
        }

        for (RdbColumn rdbColumn : columns) {
            SettingNode columnConfig = columnsConfig == null ? null : columnsConfig.getSubNode(rdbColumn.getName());
            apateColumn apateColumn = createapateColumn(apateTable, rdbColumn, columnConfig, ignoreSet, ignoreInsertSet, ignoreUpdateSet, ignoreWhereSet);
            if (apateColumn != null) {
                apateTable.addColumn(apateColumn);
            }
        }
    }

    private apateColumn createapateColumn(apateTable apateTable, RdbColumn rdbColumn, SettingNode columnConfig, //
                                          Set<String> ignoreSet, Set<String> ignoreInsertSet, Set<String> ignoreUpdateSet, Set<String> ignoreWhereSet) throws ReflectiveOperationException {
        columnConfig = (columnConfig == null) ? new TreeNode() : columnConfig;

        // try use setting create it
        SeedFactory seedFactory = this.createSeedFactory(columnConfig);
        SeedConfig seedConfig = null;
        if (seedFactory != null) {
            seedConfig = this.createSeedConfig(seedFactory, columnConfig);
        }

        // use rdbColumn create it
        TypeProcessor typeProcessor = null;
        if (seedConfig == null) {
            try {
                typeProcessor = this.typeDialect.createSeedFactory(rdbColumn, columnConfig);
            } catch (UnsupportedOperationException e) {
                logger.error(e.getMessage());
                return null;
            }
        } else {
            typeProcessor = new TypeProcessor(seedFactory, seedConfig, rdbColumn.getSqlType().getJdbcType());
        }

        // final apply form strategy
        if (!rdbColumn.hasConstraint(GeneralConstraintType.NonNull)) {
            SeedConfig config = typeProcessor.getSeedConfig();
            config.setAllowNullable(true);
            if (config.getNullableRatio() == null) {
                config.setNullableRatio(20f);
            }
        }

        // final apply form config
        Class<?> configClass = typeProcessor.getConfigType();
        List<String> properties = BeanUtils.getProperties(configClass);
        Map<String, Class<?>> propertiesMap = BeanUtils.getPropertyType(configClass);

        String[] configProperties = columnConfig.getSubKeys();
        for (String property : configProperties) {
            Object[] propertyValue = columnConfig.getSubValues(property);
            if (propertyValue == null || propertyValue.length == 0) {
                continue;
            }

            Object writeValue = (propertyValue.length == 1) ? propertyValue[0] : propertyValue;
            typeProcessor.putConfig(property, writeValue);

            Class<?> propertyType = propertiesMap.get(property);
            if (propertyType != null && propertyType.isArray()) {
                writeValue = propertyValue;
            }

            if (properties.contains(property)) {
                typeProcessor.writeProperty(property, writeValue);
            }
        }

        Set<UseFor> ignoreAct = new HashSet<>(typeProcessor.getDefaultIgnoreAct());
        ignoreAct.addAll(ignoreSet.contains(rdbColumn.getName()) ? Arrays.asList(UseFor.values()) : Collections.emptySet());
        ignoreAct.addAll(ignoreInsertSet.contains(rdbColumn.getName()) ? Collections.singletonList(UseFor.Insert) : Collections.emptySet());
        ignoreAct.addAll(ignoreUpdateSet.contains(rdbColumn.getName()) ? Collections.singletonList(UseFor.UpdateSet) : Collections.emptySet());
        ignoreAct.addAll(ignoreWhereSet.contains(rdbColumn.getName()) ? Arrays.asList(UseFor.UpdateWhere, UseFor.DeleteWhere) : Collections.emptySet());

        return new apateColumn(apateTable, rdbColumn, typeProcessor, ignoreAct, this, columnConfig);
    }

    private SeedConfig createSeedConfig(SeedFactory seedFactory, SettingNode columnConfig) {
        SeedConfig seedConfig = seedFactory.newConfig();
        for (String subKey : columnConfig.getSubKeys()) {
            String[] subValue = columnConfig.getSubValues(subKey);
            if (subValue == null || subValue.length == 0) {
                continue;
            }
            if (subValue.length == 1) {
                seedConfig.getConfigMap().put(subKey, subValue[0]);
            } else {
                seedConfig.getConfigMap().put(subKey, Arrays.asList(subValue));
            }
        }
        return seedConfig;
    }

    private SeedFactory createSeedFactory(SettingNode columnConfig) throws ReflectiveOperationException {
        String seedFactoryStr = columnConfig == null ? null : columnConfig.getSubValue(apateConfigEnum.COLUMN_SEED_FACTORY.getConfigKey());
        if (StringUtils.isNotBlank(seedFactoryStr)) {
            Class<?> seedFactoryType = this.apateConfig.getClassLoader().loadClass(seedFactoryStr);
            return (SeedFactory) seedFactoryType.newInstance();
        }

        String array = columnConfig == null ? null : columnConfig.getSubValue(apateConfigEnum.COLUMN_ARRAY_TYPE.getConfigKey());
        String seedTypeStr = columnConfig == null ? null : columnConfig.getSubValue(apateConfigEnum.COLUMN_SEED_TYPE.getConfigKey());
        SeedType seedType = SeedType.valueOfCode(seedTypeStr);
        boolean isArray = StringUtils.isNotBlank(array) && Boolean.parseBoolean(array);

        if (seedType == SeedType.Custom) {
            throw new IllegalArgumentException("custom seedType must config seedFactory.");
        } else if (seedType == SeedType.Array) {
            throw new IllegalArgumentException("arrays are specified by config.");
        }

        SeedFactory<? extends SeedConfig> factory = seedType != null ? seedType.newFactory() : null;
        if (isArray) {
            return factory == null ? null : new ArraySeedFactory(factory);
        } else {
            return factory;
        }
    }

    protected static Connection newProxyConnection(Connection connection) {
        CloseIsNothingInvocationHandler handler = new CloseIsNothingInvocationHandler(connection);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class, Closeable.class }, handler);
    }

    private static class CloseIsNothingInvocationHandler implements InvocationHandler {

        private final Connection connection;

        CloseIsNothingInvocationHandler(Connection connection){
            this.connection = connection;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            switch (method.getName()) {
                case "getTargetConnection":
                    return connection;
                case "toString":
                    return this.connection.toString();
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "close":
                    return null;
            }

            try {
                return method.invoke(this.connection, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}
