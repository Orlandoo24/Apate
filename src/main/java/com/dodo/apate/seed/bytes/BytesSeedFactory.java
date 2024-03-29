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
package com.dodo.apate.seed.bytes;

import static com.dodo.utils.RandomUtils.*;

import java.io.Serializable;
import java.util.function.Supplier;

import com.dodo.apate.seed.SeedConfig;
import com.dodo.apate.seed.SeedFactory;

/**
 * byte[] 类型的 SeedFactory
 * @version : 2022-07-25
 * @author 
 */
public class BytesSeedFactory implements SeedFactory<BytesSeedConfig> {

    @Override
    public BytesSeedConfig newConfig(SeedConfig contextType) {
        return new BytesSeedConfig();
    }

    @Override
    public Supplier<Serializable> createSeed(BytesSeedConfig seedConfig) {
        int maxLength = seedConfig.getMaxLength();
        int minLength = seedConfig.getMinLength();

        boolean allowNullable = seedConfig.isAllowNullable();
        Float nullableRatio = seedConfig.getNullableRatio();
        if (allowNullable && nullableRatio == null) {
            throw new IllegalStateException("allowNullable is true but, nullableRatio missing.");
        }

        return () -> {
            if (allowNullable) {
                if (nextFloat(0, 100) < nullableRatio) {
                    return null;
                }
            }

            int length = nextInt(minLength, maxLength);
            return nextBytes(length);
        };
    }
}
