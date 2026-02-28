package io.github.jessez332623.reactive_luascript_reader.autoconfigure;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Lua 脚本读取器属性配置类。*/
@Data
@ToString
@ConfigurationProperties(prefix = "app.luascript-reader")
public class LuaScriptReaderProperties
{
    /** 是否启用本依赖？（默认启用）*/
    private boolean enabled = true;

    /** lua 脚本根类路径。（默认为 lua-script）*/
    private String scriptRootClasspath = "lua-script";
}