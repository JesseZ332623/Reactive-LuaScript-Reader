package io.github.jessez332623.reactive_luascript_reader.impl;

import io.github.jessez332623.reactive_luascript_reader.LuaScriptReader;
import io.github.jessez332623.reactive_luascript_reader.autoconfigure.LuaScriptReaderProperties;
import io.github.jessez332623.reactive_luascript_reader.impl.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static java.lang.String.format;

/** Redis Lua 脚本读取器（实现脚本实例缓存功能）。*/
@Slf4j
final public class LuaScriptReaderImpl implements LuaScriptReader
{
    /** 合法的 Lua 脚本名构成。*/
    private static final
    Pattern LEGAL_SCRIPT_NAME = Pattern.compile("[a-zA-Z0-9_.-]+");

    /** lua 脚本根类路径。*/
    private final String scriptRootClasspath;

    /** Lua 脚本缓存：operatorType -> (scriptName -> script) */
    private final ConcurrentMap<
        LuaScriptCatalogue,
        ConcurrentMap<String, DefaultRedisScript<LuaOperatorResult>>>
        scriptCache = new ConcurrentHashMap<>();

    public LuaScriptReaderImpl(LuaScriptReaderProperties properties) {
        this.scriptRootClasspath = properties.getScriptRootClasspath();
    }

    /** 严格验证 Lua 脚本名和脚本路径。*/
    private @NotNull String
    validateClasspath(LuaScriptCatalogue operatorType, @NotNull String luaScriptName)
    {
        if (!luaScriptName.endsWith(".lua")) {
            luaScriptName = luaScriptName + ".lua";
        }

        if (!LEGAL_SCRIPT_NAME.matcher(luaScriptName).matches())
        {
            throw new
            LuaScriptSecurityException(
                format("Lua script name: %s contains illegal character!", luaScriptName),
                luaScriptName
            );
        }

        // 拼接 Lua 脚本的 classpath，
        // 格式：lua-script/{operator-type}/{script-name.lua}
        final String fullPath
            = scriptRootClasspath        +
              "/" + operatorType.getCatalogue() +
              "/" + luaScriptName;

        final String[] segments   = fullPath.split("/");
        final Deque<String> stack = new ArrayDeque<>();

        for (String segment : segments)
        {
            // 跳过空和 '.' 路径段
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }

            // 剔除路径中间的 '..'
            // 以及绝对不允许路径以 '..' 开头
            if ("..".equals(segment))
            {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
                else
                {
                    throw new
                    LuaScriptSecurityException(
                        "Lua script path not allowed start with '..'",
                        fullPath
                    );
                }
            }
            else {
                stack.add(segment);
            }
        }

        // 构造出最终的合法路径
        final String finalPath = String.join("/", stack);

        if (!finalPath.startsWith(scriptRootClasspath))
        {
            throw new
                LuaScriptSecurityException(
                format("Lua script path escapes the root directory: %s", scriptRootClasspath),
                finalPath
            );
        }

        return finalPath;
    }

    /** 从 classpath 中加载脚本。*/
    @Contract("_, _ -> new")
    private @NotNull
    DefaultRedisScript<LuaOperatorResult>
    loadFromClassPath(@NotNull LuaScriptCatalogue operatorType, String luaScriptName) throws IOException
    {
        final String scriptClasspath
            = this.validateClasspath(operatorType, luaScriptName);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptClasspath))
        {
            if (Objects.isNull(inputStream))
            {
                throw new
                LuaScriptNotFound(
                    format("Lua script: %s not exist!", scriptClasspath)
                );
            }

            return new
            DefaultRedisScript<>(
                new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
                LuaOperatorResult.class
            );
        }
    }

    /** 获取或者创建指定操作类型的脚本缓存。*/
    private ConcurrentMap<String, DefaultRedisScript<LuaOperatorResult>>
    getOrCreateScriptCache(LuaScriptCatalogue scriptOperatorType)
    {
        return
        this.scriptCache.computeIfAbsent(
            scriptOperatorType,
            (scriptName) -> new ConcurrentHashMap<>()
        );
    }

    /**
     * 通过操作类型 + 脚本名尝试从缓存中获取指定的
     * {@link DefaultRedisScript<LuaOperatorResult>}，没有则从 classpath 中加载然后缓存。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     *
     * @return 由 {@link DefaultRedisScript} 包装的，
     *         发布 Lua 脚本执行结果 {@link LuaOperatorResult} 的 {@link Mono}
     */
    private @NotNull Mono<RedisScript<LuaOperatorResult>>
    getScriptFromCache(LuaScriptCatalogue operatorType, String luaScriptName)
    {
        return
        Mono.fromCallable(() -> {
            final ConcurrentMap<String, DefaultRedisScript<LuaOperatorResult>>
                operatorCache = this.getOrCreateScriptCache(operatorType);

            // 调用 computeIfAbsent() 方法，
            // 存在则直接返回，不存在执行 mappingFunction 缓存后再返回。
            return
            operatorCache.computeIfAbsent(
                luaScriptName,
                (scriptName) -> {
                    try
                    {
                        return
                        this.loadFromClassPath(operatorType, luaScriptName);
                    }
                    catch (IOException exception)
                    {
                        throw new
                        LuaScriptLoadFailed(
                            format(
                                "Load lua script %s failed! Caused by: %s",
                                luaScriptName, exception.getMessage()),
                            exception
                        );
                    }
                }
            );
        });
    }

    /**
     * 根据配置，读取 Lua 脚本并包装。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     *
     * @return 由 {@link DefaultRedisScript} 包装的，
     *         发布 Lua 脚本执行结果 {@link LuaOperatorResult} 的 {@link Mono}
     */
    @Override
    public @NotNull Mono<RedisScript<LuaOperatorResult>>
    read(LuaScriptCatalogue operatorType, String luaScriptName)
    {
        return
        this.getScriptFromCache(operatorType, luaScriptName);
    }

    /** 清理所有缓存的 Lua 脚本。*/
    @Override
    public Mono<Integer> cleanCache()
    {
        return
        Mono.fromCallable(() -> {
            final int scripts
                = this.scriptCache.values().stream()
                      .mapToInt(Map::size)
                      .sum();

            log.info(
                "Clear cache of lua script complete! " +
                "a total {} script instances were cleared.", scripts
            );

            return scripts;
        })
        .doFinally((ignore) -> this.scriptCache.clear());
    }
}