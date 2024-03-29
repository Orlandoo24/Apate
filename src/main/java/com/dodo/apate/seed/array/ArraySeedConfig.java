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
package com.dodo.apate.seed.array;

import org.apache.ibatis.type.TypeHandler;

import com.dodo.apate.seed.SeedConfig;
import com.dodo.apate.seed.SeedType;
import com.dodo.apate.utils.types.TypeHandlerRegistryUtils;

/**
 * 数组类型 SeedConfig
 * @version : 2022-07-25
 * @author 
 */
public class ArraySeedConfig extends SeedConfig {

    private int              minSize;
    private int              maxSize;
    private final SeedConfig elementDef;

    public ArraySeedConfig(SeedConfig elementDef){
        this.elementDef = elementDef;
    }

    public final SeedType getSeedType() { return SeedType.Array; }

    @Override
    protected TypeHandler<?> defaultTypeHandler() {
        return TypeHandlerRegistryUtils.getTypeHandler(Object.class);
    }

    public int getMinSize() { return minSize; }

    public void setMinSize(int minSize) { this.minSize = minSize; }

    public int getMaxSize() { return maxSize; }

    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public SeedConfig getElementDef() { return elementDef; }
}
