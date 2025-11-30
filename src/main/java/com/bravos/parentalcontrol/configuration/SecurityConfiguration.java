package com.bravos.parentalcontrol.configuration;

import com.bravos.parentalcontrol.filter.AuthFilter;
import com.bravos.parentalcontrol.filter.BenchmarkFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthFilter authFilter, BenchmarkFilter benchmarkFilter) {
    http.authorizeHttpRequests((requests) -> requests
        .requestMatchers("/ws/**").permitAll()
        .anyRequest().authenticated());
    http.csrf(CsrfConfigurer::disable);
    http.sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
    http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(benchmarkFilter, AuthFilter.class);
    return http.build();
  }


  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration httpConfig = new CorsConfiguration();
    String allowEnv = System.getenv("ALLOW_ORIGINS");
    List<String> httpOrigins = (allowEnv == null || allowEnv.isBlank())
        ? List.of()
        : Arrays.stream(allowEnv.split(",")).map(String::trim).toList();
    httpConfig.setAllowedOrigins(httpOrigins);
    httpConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    httpConfig.setAllowedHeaders(List.of("*"));
    httpConfig.setAllowCredentials(true);

    CorsConfiguration wsConfig = new CorsConfiguration();
    wsConfig.setAllowedOriginPatterns(List.of("*"));
    wsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
    wsConfig.setAllowedHeaders(List.of("*"));
    wsConfig.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/ws/**", wsConfig);
    source.registerCorsConfiguration("/**", httpConfig);
    return source;
  }

}
