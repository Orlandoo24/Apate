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
package com.dodo.apate.seed.mysqltime;

import org.apache.ibatis.type.TypeHandler;

import com.dodo.apate.seed.date.DateSeedConfig;
import com.dodo.apate.seed.date.DateType;
import com.dodo.apate.utils.types.TypeHandlerRegistryUtils;

/**
 * 可以生成负数的时间，例如， MySQL time 类型的值范围为是 '-838:59:59.000000' to '838:59:59.000000'
 * @version : 2022-07-25
 * @author 
 */
public class MySqlTimeSeedConfig extends DateSeedConfig {

    @Override
    protected TypeHandler<?> defaultTypeHandler() {
        return TypeHandlerRegistryUtils.getTypeHandler(String.class);
    }

    @Override
    public final TypeHandler<?> getTypeHandler() { return defaultTypeHandler(); }

    @Override
    public final DateType getDateType() { return DateType.String; }
}
