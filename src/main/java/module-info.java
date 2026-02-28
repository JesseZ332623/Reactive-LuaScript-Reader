module reactive_luascript_reader
{
    // Spring 相关依赖
    requires spring.core;
    requires spring.boot;
    requires spring.context;
    requires spring.boot.autoconfigure;
    requires spring.beans;

    // Reactor 响应式编程
    requires transitive reactor.core;

    // 日志
    requires transitive org.slf4j;

    // Lombok（编译时依赖）
    requires static lombok;
    requires static org.jetbrains.annotations;

    requires transitive spring.data.redis;
    requires com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.core;

    // 开放包给 Spring 反射
    opens io.github.jessez332623.reactive_luascript_reader.autoconfigure
        to spring.core, spring.context;

    opens io.github.jessez332623.reactive_luascript_reader.impl
        to spring.beans, spring.context;
}