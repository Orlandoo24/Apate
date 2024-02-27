/*
 * Copyright 2015-2022 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.dodo.apate.generator.loader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.dodo.apate.apateConfig;
import com.dodo.apate.generator.apateColumn;
import com.dodo.apate.generator.apateTable;
import com.dodo.apate.generator.SqlArg;
import com.dodo.apate.generator.SqlPolitic;
import com.dodo.apate.utils.QueryUtils;
import com.dodo.schema.DsType;
import com.dodo.utils.CollectionUtils;
import com.dodo.utils.StringUtils;
import com.dodo.utils.jdbc.RowMapper;
import com.dodo.utils.jdbc.extractor.MultipleRowResultSetExtractor;

/**
 * 反查数据加载器
 *
 * @version : 2022-07-25
 * @author 
 */
public class DefaultDataLoaderFactory implements DataLoaderFactory {

    @Override
    public DataLoader createDataLoader(apateConfig apateConfig, Connection conn) {
        return (useFor, apateTable, batchSize) -> {
            boolean onlyKey = apateTable.getWherePolitic() == SqlPolitic.KeyCol || apateTable.getWherePolitic() == SqlPolitic.RandomKeyCol && apateTable.hasKey();
            List<String> includeColumns = new ArrayList<>();
            Map<String, String> includeColumnTerms = new HashMap<>();

            for (String col : apateTable.getColumns()) {
                apateColumn column = apateTable.findColumn(col);
                if (onlyKey && !column.isKey()) {
                    continue;
                }

                String template = column.getSelectTemplate();
                includeColumns.add(col);
                if (StringUtils.isNotBlank(template) && !StringUtils.equals(template, col)) {
                    includeColumnTerms.put(column.getColumn(), template);
                }
            }

            DsType dbType = apateConfig.getDbType();
            switch (apateConfig.getRandomMode()) {
                case RandomQuery:
                    return loadForRandomQuery(dbType, conn, apateTable, includeColumns, includeColumnTerms, batchSize);
                case RandomData:
                    return loadForRandomData(dbType, conn, apateTable, includeColumns, includeColumnTerms, batchSize);
                default:
                    throw new UnsupportedOperationException("RandomMode '" + apateConfig.getRandomMode() + "' Unsupported.");
            }
        };
    }

    protected List<Map<String, SqlArg>> loadForRandomQuery(DsType dbType, Connection conn, apateTable apateTable, //
                                                           List<String> includeColumns, Map<String, String> includeColumnTerms, int batchSize) throws SQLException {
        boolean useQualifier = apateTable.isUseQualifier();
        String catalog = apateTable.getCatalog();
        String schema = apateTable.getSchema();
        String table = apateTable.getTable();

        String queryString = QueryUtils.buildRandomQuery(dbType, useQualifier, catalog, schema, table, includeColumns, includeColumnTerms, batchSize);
        try (PreparedStatement ps = conn.prepareStatement(queryString)) {
            ResultSet resultSet = ps.executeQuery();
            return new MultipleRowResultSetExtractor<>(convertRow(apateTable, includeColumns)).extractData(resultSet);
        }
    }

    protected List<Map<String, SqlArg>> loadForRandomData(DsType dbType, Connection conn, apateTable apateTable, //
                                                          List<String> includeColumns, Map<String, String> includeColumnTerms, int batchSize) throws SQLException {
        List<Map<String, SqlArg>> resultData = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Map<String, SqlArg> record = new LinkedHashMap<>();
            for (String colName : includeColumns) {
                apateColumn col = apateTable.findColumn(colName);
                record.put(colName, col.generatorData());
            }
            resultData.add(record);
        }
        return resultData;
    }

    protected RowMapper<Map<String, SqlArg>> convertRow(apateTable apateTable, List<String> includeColumns) {
        List<String> selectColumns;
        if (CollectionUtils.isEmpty(includeColumns)) {
            selectColumns = apateTable.getColumns();
        } else {
            selectColumns = includeColumns;
        }

        return (rs, rowNum) -> {
            Map<String, SqlArg> row = new LinkedHashMap<>();
            for (String column : selectColumns) {
                apateColumn tableColumn = apateTable.findColumn(column);
                if (tableColumn == null) {
                    continue;
                }
                SqlArg result = tableColumn.readData(rs);
                row.put(column, result);
            }
            return row;
        };
    }
}
