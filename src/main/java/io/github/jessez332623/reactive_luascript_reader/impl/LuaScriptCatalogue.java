package io.github.jessez332623.reactive_luascript_reader.impl;

/**
 * 要读取的 Lua 脚本的类目录接口，
 * 使用本依赖时务必使用一个枚举类实现本接口。
 */
public interface LuaScriptCatalogue
{
    /** 获取目录字符串 */
    default String getCatalogue()
    {
        throw new
        UnsupportedOperationException(
            "Must use enum to implement LuaScriptCatalogue interface!"
        );
    }
}