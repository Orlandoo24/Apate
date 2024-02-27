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
package com.dodo.apate.utils;

import java.util.List;
import java.util.Map;

import com.dodo.schema.DsType;
import com.dodo.schema.SchemaFramework;
import com.dodo.schema.dialect.Dialect;
import com.dodo.utils.StringUtils;

/**
 * 随机查询 SqlDialect 实现
 * @version : 2020-10-31
 * @author 
 */
public class QueryUtils {

    private static String buildSelectApply(Dialect dialect, boolean useDelimited, List<String> selectColumns, Map<String, String> columnTerms) {
        StringBuilder select = new StringBuilder();
        if (selectColumns == null || selectColumns.isEmpty()) {
            select.append("*");
        } else {
            for (String col : selectColumns) {
                if (select.length() > 0) {
                    select.append(", ");
                }

                String valueTerm = columnTerms != null ? columnTerms.get(col) : null;
                if (StringUtils.isNotBlank(valueTerm)) {
                    select.append(valueTerm);
                } else {
                    select.append(dialect.fmtName(useDelimited, col));
                }
            }
        }
        return select.toString();
    }

    public static String buildRandomQuery(DsType dbType, boolean useDelimited, String catalog, String schema, String table, //
                                          List<String> columns, Map<String, String> columnTerms, int batchSize) {
        Dialect dialect = SchemaFramework.getDialect(dbType);
        String fullTableName = dialect.fmtTableName(useDelimited, catalog, schema, table);
        String selectApply = buildSelectApply(dialect, useDelimited, columns, columnTerms);

        switch (dbType) {
            case Dameng:
            case MySQL:
            case Hana:
                return "select " + selectApply + " from " + fullTableName + " order by rand() limit " + batchSize;
            case Oracle:
                return "select " + selectApply + " from (select " + selectApply + " from " + fullTableName + " order by sys_guid()) where rownum <= " + batchSize;
            case PostgreSQL:
            case PolarDBPostgre:
                return "select " + selectApply + " from " + fullTableName + " order by random() limit " + batchSize;
            case SqlServer:
                return "select top " + batchSize + " " + selectApply + " from " + fullTableName + " order by newid()";
            default:
                throw new UnsupportedOperationException("randomQuery '" + dbType + "' Unsupported.");
        }
    }
}
