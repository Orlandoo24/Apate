package com.dodo.apate.generator.parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import com.dodo.utils.ref.LinkedCaseInsensitiveMap;

/**
 * ParameterProcessor 注册器
 * @version : 2023-02-14
 * @author 
 */
public class ParameterRegistry {

    public static final ParameterRegistry                        DEFAULT             = new ParameterRegistry();
    private final Map<String, ParameterProcessor>                defaultProcessorMap = new LinkedCaseInsensitiveMap<>();
    private final Map<Class<?>, Map<String, ParameterProcessor>> typedProcessorMap   = new HashMap<>();

    private ParameterRegistry(){
    }

    static {
        ServiceLoader.load(ParameterProcessorLookUp.class).forEach(p -> p.loopUp(DEFAULT));
    }

    /** 查找 ParameterProcessor */
    public ParameterProcessor findByName(String parameterName, Class<?> seedConfigType) {
        if (seedConfigType == null) {
            return this.defaultProcessorMap.get(parameterName);
        }

        Map<String, ParameterProcessor> processorMap = this.typedProcessorMap.get(seedConfigType);
        if (processorMap == null || !processorMap.containsKey(parameterName)) {
            return this.defaultProcessorMap.get(parameterName);
        } else {
            return processorMap.get(parameterName);
        }
    }

    /** 注册 ParameterProcessor */
    public synchronized void register(String parameterName, ParameterProcessor processor) {
        register(parameterName, processor, null);
    }

    /** 注册 ParameterProcessor */
    public synchronized void register(String parameterName, ParameterProcessor processor, Class<?> withConfigType) {
        Objects.requireNonNull(processor, "processor is null.");

        if (withConfigType == null) {
            this.defaultProcessorMap.put(parameterName, processor);
        } else {
            Map<String, ParameterProcessor> processorMap = this.typedProcessorMap.get(withConfigType);
            if (processorMap == null) {
                processorMap = new LinkedCaseInsensitiveMap<>();
                this.typedProcessorMap.put(withConfigType, processorMap);
            }
            processorMap.put(parameterName, processor);
        }
    }

}
