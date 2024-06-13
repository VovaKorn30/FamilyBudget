package com.budget.planning.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    String[] allRoles = Arrays.stream(Role.values()).map(Enum::name).toArray(String[]::new);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/user/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/limit")
                                .hasAnyAuthority(Role.PARENT.toString(), Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.POST, "/user/account")
                                .hasAuthority(Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.GET, "/account/all")
                                .hasAuthority(Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.DELETE, "/account/delete")
                                .hasAuthority(Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.GET, "/account/history")
                                .hasAnyAuthority(Role.PARENT.toString(), Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.POST, "/account/**").hasAnyAuthority(allRoles)
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
