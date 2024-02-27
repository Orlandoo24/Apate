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
package com.dodo.apate.generator.processor;

import com.dodo.apate.generator.TypeProcessor;
import com.dodo.schema.umi.special.rdb.RdbColumn;

import net.hasor.cobble.setting.SettingNode;

/**
 * 读取并解析 tpc 配置文件，并根据类型和数据库信息选择对应的 tpc 配置。利用 tpc 的配置信息来创建 TypeProcessor。
 * @version : 2023-02-14
 * @author 
 */
public interface TypeProcessorFactory {

    TypeProcessor createSeedFactory(RdbColumn rdbColumn, SettingNode columnConfig) throws ReflectiveOperationException;
}
