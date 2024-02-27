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
package com.dodo.apate.generator.loader;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.dodo.apate.apateConfig;
import com.dodo.apate.generator.SqlArg;

/**
 * 反查数据加载器
 * @version : 2022-07-25
 * @author 
 */
public class PrecociousDataLoaderFactory implements DataLoaderFactory {

    @Override
    public DataLoader createDataLoader(apateConfig apateConfig, Connection conn) {
        final DataLoader defaultDataLoader = new DefaultDataLoaderFactory().createDataLoader(apateConfig, conn);
        final BlockingQueue<Map<String, SqlArg>> precociousDataSet = new LinkedBlockingQueue<>();
        final int precociousSize = 4096;

        return (useFor, apateTable, batchSize) -> {
            if (precociousSize <= 1) {
                return defaultDataLoader.loadSomeData(useFor, apateTable, batchSize);
            }

            List<Map<String, SqlArg>> result = new ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                if (precociousDataSet.size() < batchSize) {
                    synchronized (this) {
                        if (precociousDataSet.size() < batchSize) {
                            List<Map<String, SqlArg>> someData = defaultDataLoader.loadSomeData(useFor, apateTable, Math.max(precociousSize, batchSize));
                            precociousDataSet.addAll(someData);
                        }
                    }
                }

                Map<String, SqlArg> poll = precociousDataSet.poll();
                if (poll != null && !poll.isEmpty()) {
                    result.add(poll);
                }

            }
            return result;
        };
    }
}
