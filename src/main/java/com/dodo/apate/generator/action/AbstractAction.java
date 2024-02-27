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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.dodo.apate.generator.*;
import com.dodo.apate.generator.loader.DataLoader;
import com.dodo.schema.dialect.Dialect;
import com.dodo.utils.CollectionUtils;

/**
 * 公共
 * @version : 2022-07-25
 * @author 
 */
public abstract class AbstractAction implements Action {

    protected final apateTable tableInfo;
    protected final boolean    useQualifier;
    protected final Dialect    dialect;

    public AbstractAction(apateTable tableInfo, Dialect dialect){
        this.tableInfo = tableInfo;
        this.useQualifier = tableInfo.isUseQualifier();
        this.dialect = dialect;
    }

    protected final List<Map<String, SqlArg>> retryLoad(DataLoader dataLoader, UseFor useFor, apateTable apateTable, int batchSize) throws SQLException {

        int tryTimes = 0;
        while (true) {
            tryTimes++;
            try {
                List<Map<String, SqlArg>> fetchDataList = dataLoader.loadSomeData(useFor, apateTable, batchSize);
                if (CollectionUtils.isEmpty(fetchDataList)) {
                    return Collections.emptyList();
                } else {
                    return fetchDataList;
                }
            } catch (SQLException e) {
                if (tryTimes >= 3) {
                    throw e;
                }
            }
        }
    }

    protected static String[] logCols(List<apateColumn> cols) {
        return cols.stream().map(c -> c.getColumn().replace("'", "\\'")).toArray(String[]::new);
    }
}
