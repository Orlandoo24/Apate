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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import org.apache.ibatis.type.TypeHandler;

import com.dodo.apate.apateConfigEnum;
import com.dodo.apate.dsl.DslFunction;
import com.dodo.apate.dsl.DslFunctionLoopUp;
import com.dodo.apate.dsl.DslFunctionRegistry;
import com.dodo.apate.generator.UseFor;
import com.dodo.apate.generator.parameter.ParameterProcessor;
import com.dodo.apate.generator.parameter.ParameterProcessorLookUp;
import com.dodo.apate.generator.parameter.ParameterRegistry;
import com.dodo.apate.seed.enums.EnumSeedConfig;
import com.dodo.apate.seed.geometry.GeometrySeedConfig;
import com.dodo.apate.seed.number.NumberSeedConfig;
import com.dodo.apate.seed.string.StringSeedConfig;
import com.dodo.utils.NumberUtils;
import com.dodo.utils.convert.ConverterUtils;

/**
 * DslFunctionLoopUp, ParameterProcessorLookUp 的内部扩展。
 * @version : 2023-02-14
 * @author 
 */
public class PublicSpiRegistry implements DslFunctionLoopUp, ParameterProcessorLookUp {

    public static class RangeInfo {

        private final List<Object> bound;

        public RangeInfo(List<Object> bound){
            this.bound = bound;
        }

        public List<Object> getBound() { return this.bound; }
    }

    @Override
    public void loopUp(DslFunctionRegistry registry) {
        registry.register("range", dslFuncRange());
        registry.register("safeMaxLength", dslFuncSafeMaxLength());
        registry.register("min", dslFuncMin());
        registry.register("max", dslFuncMax());
        registry.register("include", dslFuncInclude());
        registry.register("ifThen", dslFuncIfThen());
        registry.register("isZero", dslFuncIsZero());
        registry.register("isNull", dslFuncIsNull());
        //        registry.register("startWith", dslFuncStartWith());
        //        registry.register("endWith", dslFuncEndWith());
    }

    // 'range(-128, 127)' DslFunction
    private static DslFunction dslFuncRange() {
        return (args, context) -> new RangeInfo(args);
    }

    // 'safeMaxLength(1, ${columnSize}, 64, 24)' DslFunction
    private static DslFunction dslFuncSafeMaxLength() {
        return (args, context) -> {
            if (args == null || args.size() < 4) {
                throw new IllegalArgumentException("safeMaxLength need 4 args.");
            }
            int minNum = (Integer) ConverterUtils.convert(Integer.TYPE, args.get(0));
            Integer confNum = args.get(1) == null ? null : (Integer) ConverterUtils.convert(Integer.class, args.get(1));
            int maxNum = (Integer) ConverterUtils.convert(Integer.TYPE, args.get(2));
            int defaultNum = (Integer) ConverterUtils.convert(Integer.TYPE, args.get(3));

            if (confNum == null) {
                return defaultNum;
            } else if (confNum < minNum) {
                return minNum;
            } else if (confNum > maxNum) {
                return maxNum;
            } else {
                return confNum;
            }
        };
    }

    // 'min(111,222)' DslFunction
    private static DslFunction dslFuncMin() {
        return (args, context) -> {
            if (args == null || args.size() < 2) {
                throw new IllegalArgumentException("min need 2 args.");
            }

            Object o1 = args.get(0);
            Object o2 = args.get(1);
            if (o1 == null || o2 == null) {
                if (o1 == null && o2 != null) {
                    return o2;
                } else if (o1 != null) {
                    return o1;
                } else {
                    throw new IllegalArgumentException("the parameter cannot be null.");
                }
            }

            Number o1Number = o1 instanceof Number ? (Number) o1 : NumberUtils.createNumber(o1.toString());
            Number o2Number = o2 instanceof Number ? (Number) o2 : NumberUtils.createNumber(o2.toString());
            return NumberUtils.lt(o1Number, o2Number) ? o1Number : o2Number;
        };
    }

    // 'max(111,222)' DslFunction
    private static DslFunction dslFuncMax() {
        return (args, context) -> {
            if (args == null || args.size() < 2) {
                throw new IllegalArgumentException("max need 2 args.");
            }

            Object o1 = args.get(0);
            Object o2 = args.get(1);
            if (o1 == null || o2 == null) {
                throw new IllegalArgumentException("the parameter cannot be null.");
            }

            Number o1Number = o1 instanceof Number ? (Number) o1 : NumberUtils.createNumber(o1.toString());
            Number o2Number = o2 instanceof Number ? (Number) o2 : NumberUtils.createNumber(o2.toString());
            return NumberUtils.gt(o1Number, o2Number) ? o1Number : o2Number;
        };
    }

    // 'include(${columnType}, 'unsigned')' DslFunction
    private DslFunction dslFuncInclude() {
        return (args, context) -> {
            if (args == null || args.size() < 2) {
                return false;
            }

            String o1 = String.valueOf(args.get(0));
            String o2 = String.valueOf(args.get(1));
            return o1.contains(o2);
        };
    }

    // 'ifThen(true, "match", "not match")' DslFunction
    private DslFunction dslFuncIfThen() {
        return (args, context) -> {
            if (args == null || args.size() < 3) {
                throw new IllegalArgumentException("min need 3 args.");
            }

            boolean test = (boolean) ConverterUtils.convert(Boolean.TYPE, args.get(0));
            return test ? args.get(1) : args.get(2);
        };
    }

    // 'isZero(<number>)' DslFunction
    private DslFunction dslFuncIsZero() {
        return (args, context) -> {
            if (args == null || args.size() != 1) {
                throw new IllegalArgumentException("need 1 args.");
            }

            if (args.get(0) == null) {
                return true;
            }

            int zeroData = (Integer) ConverterUtils.convert(Integer.TYPE, args.get(0));
            return zeroData == 0;
        };
    }

    // 'isNull(null)' DslFunction
    private DslFunction dslFuncIsNull() {
        return (args, context) -> {
            if (args == null || args.size() < 1) {
                throw new IllegalArgumentException("min need 1 args.");
            }

            for (Object obj : args) {
                if (obj != null) {
                    return false;
                }
            }
            return true;
        };
    }

    @Override
    public void loopUp(ParameterRegistry registry) {
        //
        registry.register("minMax", ppFuncMinMax(), NumberSeedConfig.class);
        registry.register("characterSet", ppFuncCharacterSet(), StringSeedConfig.class);
        registry.register("dict", ppFuncDict(), EnumSeedConfig.class);
        registry.register("range", ppFuncGeoRange(), GeometrySeedConfig.class);
        registry.register("typeHandler", ppFuncTypeHandler(), null);
        //
        registry.register("selectTemplate", ppFuncSetSetting(apateConfigEnum.SELECT_TEMPLATE), null);
        registry.register("insertTemplate", ppFuncSetSetting(apateConfigEnum.INSERT_TEMPLATE), null);
        registry.register("setColTemplate", ppFuncSetSetting(apateConfigEnum.SET_COL_TEMPLATE), null);
        registry.register("setValueTemplate", ppFuncSetSetting(apateConfigEnum.SET_VALUE_TEMPLATE), null);
        registry.register("whereColTemplate", ppFuncSetSetting(apateConfigEnum.WHERE_COL_TEMPLATE), null);
        registry.register("whereValueTemplate", ppFuncSetSetting(apateConfigEnum.WHERE_VALUE_TEMPLATE), null);
        //
        registry.register("ignoreAct", ppFuncIgnoreAct(), null);
    }

    // 'minMax' ParameterProcessor
    private static ParameterProcessor ppFuncMinMax() {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            NumberSeedConfig numSeedConf = (NumberSeedConfig) seedConfig;
            RangeInfo rangeInfo = (RangeInfo) parameter;
            int rangeArgsCnt = rangeInfo.getBound().size();

            if (!(rangeArgsCnt == 2 || rangeArgsCnt == 3)) {
                throw new IllegalArgumentException("minMax range need 2~3 args.");
            }

            boolean hasRatio = rangeArgsCnt == 3;
            int valueRatio = 0;
            BigDecimal valueMin;
            BigDecimal valueMax;

            if (hasRatio) {
                valueRatio = (int) ConverterUtils.convert(Integer.TYPE, rangeInfo.bound.get(0));
                valueMin = (BigDecimal) ConverterUtils.convert(BigDecimal.class, rangeInfo.bound.get(1));
                valueMax = (BigDecimal) ConverterUtils.convert(BigDecimal.class, rangeInfo.bound.get(2));
            } else {
                valueMin = (BigDecimal) ConverterUtils.convert(BigDecimal.class, rangeInfo.bound.get(0));
                valueMax = (BigDecimal) ConverterUtils.convert(BigDecimal.class, rangeInfo.bound.get(1));
            }

            if (!isAppend) {
                numSeedConf.clearMinMax();
            }

            if (hasRatio) {
                numSeedConf.addMinMax(valueRatio, valueMin, valueMax);
            } else {
                numSeedConf.addMinMax(valueMin, valueMax);
            }
        };
    }

    // 'characterSet' ParameterProcessor
    private static ParameterProcessor ppFuncCharacterSet() {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            StringSeedConfig strSeedConf = (StringSeedConfig) seedConfig;
            List<Object> list = (List<Object>) parameter;

            if (!isAppend) {
                strSeedConf.setCharacterSet(new HashSet<>());
            }

            for (Object obj : list) {
                strSeedConf.addCharacter(String.valueOf(obj));
            }
        };
    }

    // 'dict' ParameterProcessor
    private static ParameterProcessor ppFuncDict() {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            EnumSeedConfig enumSeedConf = (EnumSeedConfig) seedConfig;
            List<Object> list = (List<Object>) parameter;

            if (!isAppend) {
                enumSeedConf.setDict(new HashSet<>());
            }

            for (Object obj : list) {
                if (obj != null) {
                    enumSeedConf.addDict(String.valueOf(obj));
                }
            }
        };
    }

    // geo 'range' ParameterProcessor
    private static ParameterProcessor ppFuncGeoRange() {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            GeometrySeedConfig geoSeedConf = (GeometrySeedConfig) seedConfig;
            RangeInfo rangeInfo = (RangeInfo) parameter;
            int rangeArgsCnt = rangeInfo.getBound().size();

            if (!(rangeArgsCnt == 4 || rangeArgsCnt == 5)) {
                throw new IllegalArgumentException("minMax range need 4~5 args.");
            }

            boolean hasRatio = rangeArgsCnt == 5;
            int valueRatio = 0;
            double axisXofA;
            double axisYofA;
            double axisXofB;
            double axisYofB;

            if (hasRatio) {
                valueRatio = (int) ConverterUtils.convert(Integer.TYPE, rangeInfo.bound.get(0));
                axisXofA = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(1));
                axisYofA = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(2));
                axisXofB = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(3));
                axisYofB = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(4));
            } else {
                axisXofA = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(0));
                axisYofA = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(1));
                axisXofB = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(2));
                axisYofB = (double) ConverterUtils.convert(Double.TYPE, rangeInfo.bound.get(3));
            }

            if (!isAppend) {
                geoSeedConf.getRange().clearRatio();
            }

            if (hasRatio) {
                geoSeedConf.addRange(valueRatio, axisXofA, axisYofA, axisXofB, axisYofB);
            } else {
                geoSeedConf.addRange(axisXofA, axisYofA, axisXofB, axisYofB);
            }
        };
    }

    // seedConfig`s ParameterProcessor
    private ParameterProcessor ppFuncSetSetting(final apateConfigEnum settingKey) {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            if (parameter == null) {
                return;
            }
            colSetting.setValue(settingKey.getConfigKey(), String.valueOf(parameter));
        };
    }

    // 'typeHandler' ParameterProcessor
    private ParameterProcessor ppFuncTypeHandler() {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            if (parameter == null) {
                return;
            }

            ClassLoader classLoader = apateConfig.getClassLoader();
            Class<?> typeHandlerType = classLoader.loadClass(parameter.toString());
            TypeHandler<?> instance = (TypeHandler<?>) typeHandlerType.newInstance();
            seedConfig.setTypeHandler(instance);
        };
    }

    // 'ignoreAct' ParameterProcessor
    private ParameterProcessor ppFuncIgnoreAct() {
        return (apateConfig, colMeta, colSetting, seedConfig, typeProcessor, isAppend, parameter) -> {
            if (parameter == null) {
                return;
            }

            if (!isAppend) {
                typeProcessor.getDefaultIgnoreAct().clear();
            }

            List<Object> list = (List<Object>) parameter;

            for (Object obj : list) {
                UseFor useFor = (UseFor) ConverterUtils.convert(UseFor.class, obj);
                if (useFor != null) {
                    typeProcessor.getDefaultIgnoreAct().add(useFor);
                }
            }
        };
    }
}
