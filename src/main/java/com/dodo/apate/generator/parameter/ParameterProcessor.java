package com.dodo.apate.generator.parameter;

import com.dodo.apate.apateConfig;
import com.dodo.apate.generator.TypeProcessor;
import com.dodo.apate.seed.SeedConfig;
import com.dodo.schema.umi.special.rdb.RdbColumn;

import net.hasor.cobble.setting.SettingNode;

/**
 * 自定义参数配置方式
 * @version : 2023-02-14
 * @author 
 */
public interface ParameterProcessor {

    void processor(apateConfig apateConfig, RdbColumn colMeta, SettingNode colSetting, //
                   SeedConfig seedConfig, TypeProcessor typeProcessor, boolean isAppend, Object parameter) throws ReflectiveOperationException;
}
