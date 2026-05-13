package com.stockpro.stockmovement.config;

import com.stockpro.stockmovement.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    
    
    

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            //  Enable CORS properly
            .cors(cors -> cors.disable())

            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // VERY IMPORTANT (preflight fix)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                //  Allow actuator
                .requestMatchers("/actuator/**").permitAll()

                //  Allow Swagger
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                //  Whitelisting record for internal Feign calls (Warehouse Service)
                .requestMatchers("/api/v1/movements/record").permitAll()

                //  DO NOT keep movements public in final version
                .requestMatchers("/api/v1/movements/**").authenticated()

                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
