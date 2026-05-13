package com.stockpro.alert.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FeignConfig — adds X-Internal-Request header to all Feign calls
 * so microservice security filters allow the request without JWT.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalRequestInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Request", "true");
    }
}
