package io.github.jessez332623.reactive_luascript_reader.impl.exception;

/** 加载 Lua 脚本失败时抛出本异常。*/
public class LuaScriptLoadFailed extends RuntimeException
{
    public LuaScriptLoadFailed(String message) {
        super(message);
    }

    public LuaScriptLoadFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
