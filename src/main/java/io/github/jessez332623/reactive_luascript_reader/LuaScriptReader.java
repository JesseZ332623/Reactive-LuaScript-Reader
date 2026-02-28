package io.github.jessez332623.reactive_luascript_reader;

import io.github.jessez332623.reactive_luascript_reader.impl.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

/** Redis Lua 脚本读取器接口。*/
public interface LuaScriptReader
{
    /**
     * 根据配置，读取 Lua 脚本并包装。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     *
     * @return 由 {@link DefaultRedisScript} 包装的，
     *         发布 Lua 脚本执行结果 {@link LuaOperatorResult} 的 {@link Mono}
     */
    @NotNull Mono<RedisScript<LuaOperatorResult>>
    read(LuaScriptCatalogue operatorType, String luaScriptName);

    /**
     * 清理所有缓存的 Lua 脚本。
     *
     * @return 清理调的缓存的脚本数量
     */
    Mono<Integer> cleanCache();
}