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
package com.dodo.apate.seed;

import java.util.function.Supplier;

import com.dodo.apate.seed.array.ArraySeedFactory;
import com.dodo.apate.seed.bool.BooleanSeedFactory;
import com.dodo.apate.seed.bytes.BytesSeedFactory;
import com.dodo.apate.seed.date.DateSeedFactory;
import com.dodo.apate.seed.enums.EnumSeedFactory;
import com.dodo.apate.seed.geometry.GeometrySeedFactory;
import com.dodo.apate.seed.guid.GuidSeedFactory;
import com.dodo.apate.seed.number.NumberSeedFactory;
import com.dodo.apate.seed.string.StringSeedFactory;

import net.hasor.cobble.StringUtils;

/**
 * 类型
 * @version : 2022-07-25
 * @author
 */
public enum SeedType {

    Boolean(BooleanSeedFactory::new),
    Date(DateSeedFactory::new),
    String(StringSeedFactory::new),
    Number(NumberSeedFactory::new),
    Enums(EnumSeedFactory::new),
    Bytes(BytesSeedFactory::new),
    GID(GuidSeedFactory::new),
    Array(ArraySeedFactory::new),
    Geometry(GeometrySeedFactory::new),
    //    Struts,
    //    RelationId,
    Custom(null);

    private final Supplier<SeedFactory<? extends SeedConfig>> supplier;

    SeedType(Supplier<SeedFactory<? extends SeedConfig>> supplier){
        this.supplier = supplier;
    }

    public SeedFactory<? extends SeedConfig> newFactory() {
        return this.supplier != null ? this.supplier.get() : null;
    }

    public static SeedType valueOfCode(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (SeedType seedType : SeedType.values()) {
            if (StringUtils.equalsIgnoreCase(seedType.name(), name)) {
                return seedType;
            }
        }
        return null;
    }
}
