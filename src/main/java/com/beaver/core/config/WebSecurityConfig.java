package com.beaver.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // disables csrf token requirement
                .authorizeHttpRequests(request -> request
                                .requestMatchers("/register/**", "/login/**").permitAll()
                                .anyRequest().authenticated()) // enforces authentication on all request
                .httpBasic(Customizer.withDefaults()); // adds basic http authentication
        return httpSecurity.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails connor = User.withUsername("connor")
                .password("{noop}password")
                .roles("USER")
                .build();

        UserDetails kyndall = User.withUsername("kyndall")
                .password("{noop}password")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(connor, kyndall);
    }
}
