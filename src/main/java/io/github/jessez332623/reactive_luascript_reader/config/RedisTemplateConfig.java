package io.github.jessez332623.reactive_luascript_reader.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.jessez332623.reactive_luascript_reader.impl.LuaOperatorResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 专用 Redis 模板配置类，为保持依赖友好，加载规则如下：
 * <ul>
 *     <li>确保在 spring-data-redis 依赖存在时才加载</li>
 *     <li>确保配置了 {@link ReactiveRedisConnectionFactory} 时才加载</li>
 *     <li>确保配置了 {@link ObjectMapper} 时才加载</li>
 *     <li>如果使用者已经定义了相同名字的 Bean，不加载</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(ReactiveRedisTemplate.class)
@ConditionalOnBean({
    ReactiveRedisConnectionFactory.class,
    ObjectMapper.class
})
@ConditionalOnMissingBean(name = "luascript-reactive-redis-template")
public class RedisTemplateConfig
{
    /** 专门用于执行 Lua 脚本的响应式 Redis 模板。*/
    @Bean(name = "luascript-reactive-redis-template")
    public ReactiveRedisTemplate<String, LuaOperatorResult>
    redisLuaScriptTemplate(
        final ReactiveRedisConnectionFactory factory,
        final ObjectMapper objectMapper
    )
    {
        final RedisSerializer<String> keySerializer = new StringRedisSerializer();

        final Jackson2JsonRedisSerializer<LuaOperatorResult> valueSerializer
            = new Jackson2JsonRedisSerializer<>(
                objectMapper.copy()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                LuaOperatorResult.class
        );

        final RedisSerializationContext<String, LuaOperatorResult> context
            = RedisSerializationContext.<String, LuaOperatorResult>
                newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new
        ReactiveRedisTemplate<>(factory, context);
    }
}