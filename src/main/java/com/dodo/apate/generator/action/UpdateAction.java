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
package com.dodo.apate.generator.action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dodo.apate.OpsType;
import com.dodo.apate.generator.*;
import com.dodo.apate.generator.loader.DataLoader;
import com.dodo.schema.dialect.Dialect;
import com.dodo.utils.RandomUtils;
import com.dodo.utils.StringUtils;

/**
 * UPDATE 生成器
 * @version : 2022-07-25
 * @author 
 */
public class UpdateAction extends AbstractAction {

    private final List<apateColumn> updateSetColumns;
    private final List<apateColumn> whereFullColumns;
    private final List<apateColumn> whereKeyColumns;
    private final DataLoader        dataLoader;

    public UpdateAction(apateTable tableInfo, Dialect dialect, List<apateColumn> updateSetColumns, List<apateColumn> whereColumns, DataLoader dataLoader){
        super(tableInfo, dialect);
        this.dataLoader = dataLoader;
        this.updateSetColumns = updateSetColumns;
        this.whereFullColumns = whereColumns;
        this.whereKeyColumns = whereColumns.stream().filter(apateColumn::isKey).collect(Collectors.toList());
    }

    @Override
    public List<BoundQuery> generatorAction(int batchSize) throws SQLException {
        List<apateColumn> setColumns = null;
        switch (this.tableInfo.getUpdateSetPolitic()) {
            case RandomKeyCol:
            case RandomCol: {
                setColumns = new ArrayList<>(this.updateSetColumns);
                if (!this.updateSetColumns.isEmpty()) {
                    List<apateColumn> cutColumns = randomCol(this.updateSetColumns);
                    setColumns.removeAll(cutColumns);
                }

                // maker sure is not empty set.
                if (setColumns.isEmpty() && !this.updateSetColumns.isEmpty()) {
                    if (this.updateSetColumns.size() == 1) {
                        setColumns.add(this.updateSetColumns.get(0));
                    } else {
                        setColumns.add(this.updateSetColumns.get(RandomUtils.nextInt(0, this.updateSetColumns.size() - 1)));
                    }
                }
                break;
            }
            case KeyCol:
            case FullCol:
                setColumns = this.updateSetColumns;
                break;
            default:
                throw new UnsupportedOperationException("updateSetPolitic '" + this.tableInfo.getInsertPolitic() + "' Unsupported.");
        }

        List<apateColumn> whereColumns = null;
        switch (this.tableInfo.getWherePolitic()) {
            case RandomKeyCol:
                if (!this.whereKeyColumns.isEmpty()) {
                    whereColumns = randomCol(whereKeyColumns);
                    break;
                }
            case RandomCol:
                whereColumns = randomCol(whereFullColumns);
                break;
            case KeyCol:
                if (!this.whereKeyColumns.isEmpty()) {
                    whereColumns = whereKeyColumns;
                    break;
                }
            case FullCol:
                whereColumns = whereFullColumns;
                break;
            default:
                throw new UnsupportedOperationException("updateWherePolitic '" + this.tableInfo.getWherePolitic() + "' Unsupported.");
        }

        return buildAction(batchSize, setColumns, whereColumns);
    }

    private List<apateColumn> randomCol(List<apateColumn> useCols) {
        List<apateColumn> useColumns = new ArrayList<>(useCols);

        int maxCut = RandomUtils.nextInt(0, useColumns.size());
        for (int i = 0; i < maxCut; i++) {
            if (useColumns.size() == 1) {
                useColumns.remove(0);
            } else {
                useColumns.remove(RandomUtils.nextInt(0, useColumns.size() - 1));
            }
        }

        // maker sure is not empty delete.
        if (useColumns.isEmpty() && !useCols.isEmpty()) {
            if (useCols.size() == 1) {
                useColumns.add(useCols.get(0));
            } else {
                useColumns.add(useCols.get(RandomUtils.nextInt(0, useCols.size() - 1)));
            }
        }

        return useColumns;
    }

    private List<BoundQuery> buildAction(int batchSize, List<apateColumn> setColumns, List<apateColumn> whereColumns) throws SQLException {
        // fetch some data used for delete
        List<Map<String, SqlArg>> fetchDataList = this.retryLoad(this.dataLoader, UseFor.UpdateWhere, this.tableInfo, batchSize);
        if (fetchDataList == null || fetchDataList.isEmpty()) {
            return Collections.emptyList();
        }

        // build delete sql
        String catalog = this.tableInfo.getCatalog();
        String schema = this.tableInfo.getSchema();
        String table = this.tableInfo.getTable();
        String tableName = this.dialect.fmtTableName(this.useQualifier, catalog, schema, table);

        // build set
        StringBuilder set = new StringBuilder();
        for (apateColumn colInfo : setColumns) {
            if (set.length() > 0) {
                set.append(", ");
            }
            set.append(this.dialect.fmtName(this.useQualifier, colInfo.getColumn()));
            set.append(" = ");
            set.append(colInfo.getSetValueTemplate());
        }

        // build where
        StringBuilder where = new StringBuilder();
        for (apateColumn colInfo : whereColumns) {
            if (where.length() > 0) {
                where.append(" and ");
            }
            where.append(colInfo.getWhereColTemplate());
            where.append(" = ");
            where.append(colInfo.getWhereValueTemplate());
        }
        StringBuilder builder = new StringBuilder();
        builder.append("update " + tableName);
        builder.append(" set " + set);
        builder.append(" where " + where);

        // build args
        List<BoundQuery> boundQueries = new ArrayList<>();
        for (Map<String, SqlArg> objectMap : fetchDataList) {
            SqlArg[] args = new SqlArg[setColumns.size() + whereColumns.size()];
            int index = 0;
            for (apateColumn colInfo : setColumns) {
                args[index++] = colInfo.generatorData();
            }
            for (apateColumn colInfo : whereColumns) {
                args[index++] = objectMap.get(colInfo.getColumn());
            }

            boundQueries.add(new BoundQuery(this.tableInfo, OpsType.Update, builder, args));
        }
        return boundQueries;
    }

    @Override
    public String toString() {
        SqlPolitic updatePolitic = this.tableInfo.getUpdateSetPolitic();
        String setCols = "'" + StringUtils.join(logCols(this.updateSetColumns), "','") + "'";
        switch (updatePolitic) {
            case KeyCol:
            case RandomKeyCol:
                String whereKCols = "'" + StringUtils.join(logCols(this.whereKeyColumns), "','") + "'";
                return "UpdateAct{politic='" + updatePolitic + "'," + "setCols= [" + setCols + "], whereCols= [" + whereKCols + "]}";
            case FullCol:
            case RandomCol:
                String whereFCols = "'" + StringUtils.join(logCols(this.whereFullColumns), "','") + "'";
                return "UpdateAct{politic='" + updatePolitic + "'," + "setCols= [" + setCols + "], whereCols= [" + whereFCols + "]}";
            default:
                return "UpdateAct{politic='" + updatePolitic + "'," + "setCols= [" + setCols + "], whereCols= 'unknown'}";
        }
    }
}
