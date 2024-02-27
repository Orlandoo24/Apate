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
package com.dodo.apate.seed.enums;

import static com.dodo.utils.RandomUtils.nextFloat;
import static com.dodo.utils.RandomUtils.nextInt;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Supplier;

import com.dodo.apate.seed.SeedConfig;
import com.dodo.apate.seed.SeedFactory;

/**
 * 枚举类型的 SeedFactory
 * @version : 2022-07-25
 * @author 
 */
public class EnumSeedFactory implements SeedFactory<EnumSeedConfig> {

    @Override
    public EnumSeedConfig newConfig(SeedConfig contextType) {
        return new EnumSeedConfig();
    }

    @Override
    public Supplier<Serializable> createSeed(EnumSeedConfig seedConfig) {
        boolean allowNullable = seedConfig.isAllowNullable();
        Float nullableRatio = seedConfig.getNullableRatio();
        if (allowNullable && nullableRatio == null) {
            throw new IllegalStateException("allowNullable is true but, nullableRatio missing.");
        }

        Set<String> dict = seedConfig.getDict();
        if (allowNullable) {
            dict.add(null);
        }

        String[] dictArrays = dict.toArray(new String[0]);
        int max = dictArrays.length;

        return () -> {
            if (allowNullable && nextFloat(0, 100) < nullableRatio) {
                return null;
            } else {
                return (dictArrays.length == 0) ? null : (dictArrays.length == 1) ? dictArrays[0] : dictArrays[nextInt(0, max)];
            }
        };
    }
}
