package com.richardbrenkus.hospitalshiftscheduler.security;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/403", "/styles/**", "/css/**", "/js/**", "/images/**", "/language/**").permitAll()
                        .requestMatchers("/", "/home", "/index").authenticated()
                        .requestMatchers("/admin/**").hasAnyRole(Role.ADMIN.name(), Role.DEMO_ADMIN.name())
                        .requestMatchers("/user/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.DEMO_ADMIN.name())
                        .anyRequest().authenticated()
                )
                //.csrf(AbstractHttpConfigurer::disable)
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect(request.getContextPath() + "/403"))
                );

        return http.build();
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }
}