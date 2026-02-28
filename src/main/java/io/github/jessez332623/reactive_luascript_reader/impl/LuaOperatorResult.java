package io.github.jessez332623.reactive_luascript_reader.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.jetbrains.annotations.Nullable;

/** Lua 脚本的执行结果是一个 JSON 类型，会被映射成该 POJO。*/
@Getter
@ToString
public class LuaOperatorResult
{
    /** Lua 脚本执行状态。*/
    private final String status;

    /** Lua 脚本执行结果信息（可以为 null）。*/
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String message;

    /**
     * Lua 脚本读取的数据
     *（JSON 字符串形式，应用层要自行完成序列化，可以为 null）
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String data;

    /** Lua 脚本执行完成时的时间戳。*/
    private final long timestamp;

    /** Jackson 专用构造器 */
    @JsonCreator
    public LuaOperatorResult(
        @JsonProperty("status")    String status,
        @JsonProperty("message")   @Nullable String message,
        @JsonProperty("data")      @Nullable String data,
        @JsonProperty("timestamp") long timestamp
    )
    {
        this.status    = status;
        this.message   = message;
        this.data      = data;
        this.timestamp = timestamp;
    }
}