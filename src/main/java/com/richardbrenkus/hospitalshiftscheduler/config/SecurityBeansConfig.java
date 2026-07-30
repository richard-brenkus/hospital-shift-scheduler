package com.richardbrenkus.hospitalshiftscheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public AuthenticationTrustResolver authenticationTrustResolver() {
        return new AuthenticationTrustResolverImpl();
    }
}
