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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.dodo.apate.OpsType;
import com.dodo.apate.generator.*;
import com.dodo.schema.dialect.Dialect;
import com.dodo.utils.RandomUtils;
import com.dodo.utils.StringUtils;

/**
 * INSERT 生成器
 * @version : 2022-07-25
 * @author 
 */
public class InsertAction extends AbstractAction {

    private final List<apateColumn> insertColumns;
    private final List<apateColumn> canCutColumns;

    public InsertAction(apateTable tableInfo, Dialect dialect, List<apateColumn> insertColumns){
        super(tableInfo, dialect);
        this.insertColumns = insertColumns;
        this.canCutColumns = insertColumns.stream().filter(apateColumn::isCanBeCut).collect(Collectors.toList());
    }

    @Override
    public List<BoundQuery> generatorAction(int batchSize) {
        switch (this.tableInfo.getInsertPolitic()) {
            case KeyCol:
            case RandomKeyCol:
            case RandomCol:
                return generatorByRandom(batchSize);
            case FullCol:
                return generatorByFull(batchSize);
            default:
                throw new UnsupportedOperationException("insertPolitic '" + this.tableInfo.getInsertPolitic() + "' Unsupported.");
        }
    }

    private List<BoundQuery> generatorByRandom(int batchSize) {
        // try use cut
        List<apateColumn> useColumns = new ArrayList<>(this.insertColumns);
        List<apateColumn> cutColumns = new ArrayList<>();

        int maxCut = RandomUtils.nextInt(0, this.canCutColumns.size() - 1);
        while (cutColumns.size() < maxCut) {
            apateColumn cutColumn;
            if (this.canCutColumns.size() == 1) {
                cutColumn = this.canCutColumns.get(0);
            } else {
                cutColumn = this.canCutColumns.get(RandomUtils.nextInt(0, maxCut));
            }

            if (!cutColumns.contains(cutColumn)) {
                cutColumns.add(cutColumn);
            }
        }
        useColumns.removeAll(cutColumns);

        // maker sure is not empty insert.
        if (useColumns.isEmpty() && !this.canCutColumns.isEmpty()) {
            if (this.canCutColumns.size() == 1) {
                useColumns.add(this.canCutColumns.get(0));
            } else {
                useColumns.add(this.canCutColumns.get(RandomUtils.nextInt(0, this.canCutColumns.size() - 1)));
            }
        }

        return buildAction(batchSize, useColumns);
    }

    private List<BoundQuery> generatorByFull(int batchSize) {
        return buildAction(batchSize, this.insertColumns);
    }

    private List<BoundQuery> buildAction(int batchSize, List<apateColumn> useColumns) {
        String catalog = this.tableInfo.getCatalog();
        String schema = this.tableInfo.getSchema();
        String table = this.tableInfo.getTable();
        String tableName = this.dialect.fmtTableName(this.useQualifier, catalog, schema, table);

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (apateColumn colInfo : useColumns) {
            if (columns.length() > 0) {
                columns.append(", ");
                values.append(", ");
            }
            String colName = colInfo.getColumn();
            columns.append(this.dialect.fmtName(this.useQualifier, colName));
            values.append(colInfo.getInsertTemplate());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("insert into " + tableName);
        builder.append("(" + columns + ")");
        builder.append(" values ");
        builder.append("(" + values + ")");

        List<BoundQuery> boundQueries = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            SqlArg[] args = new SqlArg[useColumns.size()];
            for (int argIdx = 0; argIdx < useColumns.size(); argIdx++) {
                apateColumn colInfo = useColumns.get(argIdx);
                args[argIdx] = colInfo.generatorData();
            }

            boundQueries.add(new BoundQuery(this.tableInfo, OpsType.Insert, builder, args));
        }
        return boundQueries;
    }

    @Override
    public String toString() {
        SqlPolitic insertPolitic = this.tableInfo.getInsertPolitic();
        String insertCols = "'" + StringUtils.join(logCols(this.insertColumns), "','") + "'";
        return "InsertAct{politic='" + insertPolitic + "'," + "insertCols= [" + insertCols + "]}";
    }
}
