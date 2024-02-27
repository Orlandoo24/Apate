package com.dodo.apate.utils.types;

import java.util.Map;

import org.apache.ibatis.type.TypeHandler;

/**
 * TypeHandler 的工厂。
 * @version : 2023-02-14
 * @author 
 */
public interface TypeHandlerFactory {

    TypeHandler<?> createTypeHandler(Map<String, Object> context);
}
