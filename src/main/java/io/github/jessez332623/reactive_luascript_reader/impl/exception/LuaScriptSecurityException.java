package io.github.jessez332623.reactive_luascript_reader.impl.exception;

import lombok.Getter;

/** 非法 Lua 脚本路径异常。*/
public class LuaScriptSecurityException extends RuntimeException
{
    @Getter
    private final String illegalPath; // 记录非法路径。

    public LuaScriptSecurityException(String message, String illegalPath)
    {
        super(message);
        this.illegalPath = illegalPath;
    }
}