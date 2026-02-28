package io.github.jessez332623.reactive_luascript_reader.impl.exception;

import lombok.Getter;

/** 当 Lua 脚本执行失败时，抛出本异常。*/
public class LuaScriptExecuteFailed extends RuntimeException
{
    /** 错误类型。*/
    @Getter
    private final String errorType;

    /** 错误发生时记录的时间戳。*/
    @Getter
    private final long timestamp;

    public LuaScriptExecuteFailed(
        String errorType, String message, long timestamp)
    {
        super(message);

        this.errorType = errorType;
        this.timestamp = timestamp;
    }
}
