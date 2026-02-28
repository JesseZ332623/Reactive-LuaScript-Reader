package io.github.jessez332623.reactive_luascript_reader.impl.exception;

/** 在指定文件目录下找不到指定 Lua 脚本时抛出本异常。*/
public class LuaScriptNotFound extends RuntimeException
{
    public LuaScriptNotFound(String message) {
        super(message);
    }

    public LuaScriptNotFound(String message, Throwable throwable) {
        super(message, throwable);
    }
}