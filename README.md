# 响应式 Lua 脚本读取器

使用更安全的方式读取 Lua 脚本，供 ReactiveRedisTemplate<> 执行。

## 用法示例

务必使用 enum 先实现 `LuaScriptCatalogue` 接口，
它用于区分不同的 Lua 脚本目录，示例如下：

```java
@RequiredArgsConstructor
public enum LuaScriptCatalogueEnum implements LuaScriptCatalogue
{
    /** 邮件限流相关的脚本。*/
    EMAIL_SEND("email-send"),

    /** 其他脚本。*/
    OTHERS("others");

    private final String catalogue;

    @Override
    public String getCatalogue() {
        return this.catalogue;
    }
}
```

在项目的 `resource/lua-script/` 目录下编写 Lua 脚本，
这里分享一个基于令牌桶策略邮件限流 Lua 脚本 `trafficLimiting.lua`：

```lua
--[[
    使用令牌桶策略对邮件的发送进行限流。

    KEYS:
        key  邮件发送限流键前缀 (sql-monitor-mail-rate:<db-host>)

    ARGV:
        rate  (填充桶的速率，单位：令牌/秒，如 0.1667 即为 10 令牌每分钟)
        burst 桶的容量
]]

local key    = KEYS[1]

local rate   = tonumber(ARGV[1])
local burst  = ARGV[2]

-- 当前时间戳（秒）
local now = tonumber(redis.call('TIME')[1])

-- 获取当前时间戳
local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 +
           math.floor(tonumber(redisTime[2]) / 1000)
end

-- 动态的 TTL 设置（最多不超过 1 天，最少不超过 60 秒）
local function getSafeTTL(tokens)
    -- 公式为：填满桶需要的时间 * 2 + 300 秒缓冲
    -- 这样不活跃的桶会更快过期，活跃的会自动续期
    local timeToFull = (burst - tokens) / rate
    if timeToFull < 0 then timeToFull = 0 end
    return math.max(60, math.min(86400, math.floor(timeToFull * 2 + 300)))
end

local status, result = pcall(
        function()
            -- 获取当前桶中的令牌数（第一次执行时则填满桶）
            local tokens = tonumber(redis.call('HGET', key, 'tokens') or burst)

            -- 获取上次填充令牌桶的时间
            local last   = tonumber(redis.call('HGET', key, 'last') or now)

            -- 需要补充的令牌数 = 两次填充时间之差 * 填充速率
            local added = (now - last) * rate

            -- 将令牌数强制限制在 [0, burst] 内，避免浮点精度问题
            tokens = math.max(0, math.min(burst, tokens + added))

            -- 尝试消耗 1 个令牌
            if tokens >= 1 then
                tokens = math.max(0, tokens - 1)

                -- 更新记录
                redis.call('HSET', key, 'tokens', tokens, 'last', now)
                redis.call('EXPIRE', key, getSafeTTL(tokens))

                -- 放行
                return {
                    status  = "SEND_PASS",
                    message = nil,
                    timestamp = getTimestamp()
                }
            else
                redis.call('EXPIRE', key, getSafeTTL(tokens))
                -- 拒绝
                return {
                    status  = "SEND_REJECT",
                    message = nil,
                    timestamp = getTimestamp()
                }
            end
        end
)

if status then
    -- result 是 table（成功或业务失败）
    return cjson.encode(result)
else
    -- Redis 服务端级别的错误
    return cjson.encode({
        status    = "UNKNOWN_ERROR",
        message   = tostring(result),
        timestamp = getTimestamp()
    })
end
```

本依赖准备了一个专用于执行 Lua 脚本的 `ReactiveRedisTemplate<>` 代码见：

- [专门用于执行 Lua 脚本的响应式 Redis 模板](https://github.com/JesseZ332623/Reactive-LuaScript-Reader/blob/main/src/main/java/io/github/jessez332623/reactive_luascript_reader/config/RedisTemplateConfig.java)

读取 Lua 脚本并执行，对不同的结果做不同的处理：

```java
/**
  * 采用令牌桶策略对邮件发送进行限流（限流参数通过配置给出）。
  *
  * @param executor 任务的执行者是？
 */
private @NotNull Mono<Void>
sendTrafficLimiting(@NotNull TaskExecutor executor)
{
    // 定时任务不受限流的约束
    if (executor.equals(TaskExecutor.AUTO_TASK)) {
        return Mono.empty();
    }

    return 
    this.luaScriptReader
        .read(LuaScriptCatalogueEnum.EMAIL_SEND, "trafficLimiting.lua")
        .flatMap((script) -> {
            final String keyPrefix    = this.trafficLimitKeyPrefix();
            final int    fillTokens   = this.emailTrafficLimitingProps.getFillTokens();
            final long   fillDuration = this.emailTrafficLimitingProps.getFillDuration().toSeconds();
            final double rate         = (double) fillTokens / fillDuration;
            final int    burst        = this.emailTrafficLimitingProps.getBurst();

            return 
            this.luaScriptTemplate
                .execute(script, List.of(keyPrefix), rate, burst)
                .next()
                .flatMap((result) ->
                    switch (result.getStatus())
                    {
                        case "SEND_PASS" -> Mono.empty();

                        case "SEND_REJECT" ->
                            Mono.error(
                                new ScheduledTasksException(
                                    "The number of attempts has exceeded the limit. Please try again later."
                                )
                            );

                        case "UNKNOWN_ERROR" ->
                            Mono.error(new ScheduledTasksException(result.getMessage()));

                        // 不可到达的
                        default ->
                            Mono.error(
                                new IllegalStateException(
                                    "Unexpected value: " + result.getStatus()
                                )
                            );
                    });
        });
}
```

## 属性配置

```yaml
app:
  # 是否启用本依赖？（默认启用）
  luascript-reader:
    enabled: true
  
  # lua 脚本根类路径。（默认为 lua-script）
  script-root-classpath: lua-script
```

# LICENCE

[Apache License Version 2.0](https://github.com/JesseZ332623/Reactive-LuaScript-Reader/blob/main/LICENSE.txt)

---

2026.02.28