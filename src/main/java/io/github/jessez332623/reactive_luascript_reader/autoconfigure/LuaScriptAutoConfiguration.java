package io.github.jessez332623.reactive_luascript_reader.autoconfigure;

import io.github.jessez332623.reactive_luascript_reader.impl.LuaScriptReaderImpl;
import io.github.jessez332623.reactive_luascript_reader.LuaScriptReader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix         = "app.luascript-reader",
    name           = "enabled",
    havingValue    = "true",
    matchIfMissing = true // 默认启用本依赖
)
@RequiredArgsConstructor
@EnableConfigurationProperties(value = {LuaScriptReaderProperties.class})
public class LuaScriptAutoConfiguration
{
    private final LuaScriptReaderProperties properties;

    @Bean
    @ConditionalOnMissingBean(value = {LuaScriptReaderImpl.class})
    public LuaScriptReader luaScriptReader() {
        return new LuaScriptReaderImpl(properties);
    }
}