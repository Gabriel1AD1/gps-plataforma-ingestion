package com.ingestion.pe.mscore.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.ingestion.pe.mscore.commons.models.enums.Role;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

  private final SecurityFilter securityFilter;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            authorize -> authorize
                .requestMatchers(EndpointSecurityConstant.ENDPOINT_PUBLIC)
                .permitAll()
                .requestMatchers(EndpointSecurityConstant.ENDPOINT_PRIVATE)
                .authenticated()
                .requestMatchers(EndpointSecurityConstant.ENDPOINT_SWAGGER)
                .hasAnyAuthority(
                    Role.SUPPORT.name(), Role.ADMIN.name(), Role.SUPPER_ADMIN.name())
                .anyRequest()
                .authenticated())
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint))
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
  }


}
