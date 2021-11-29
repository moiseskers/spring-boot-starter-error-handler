package com.generic.springbootstartererrorhandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "error.handler")
public class Config {

    @Bean
    @ConditionalOnProperty("error.handler.enabled")
    public GlobalExceptionHandler enable() {
        return new GlobalExceptionHandler();
    }
}
