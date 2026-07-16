package com.werkpilot.shared.security;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter) throws Exception {
        String[] masterDataPaths = {
                "/factories/**",
                "/production-lines/**",
                "/machines/**",
                "/products/**",
                "/shifts/**",
                "/downtime-reasons/**",
                "/scrap-categories/**"
        };

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/password-reset-request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/password-reset-confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs").permitAll()
                        .requestMatchers("/api/v1/__contract/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/audit-events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/import-jobs/production-records").hasAnyRole("ADMIN", "PRODUCTION_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/import-jobs/energy-measurements").hasAnyRole("ADMIN", "ENERGY_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/import-jobs/downtime-records").hasAnyRole("ADMIN", "PRODUCTION_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/import-jobs/scrap-records").hasAnyRole("ADMIN", "PRODUCTION_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/import-jobs/*/correction").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/import-jobs/*/rollback").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/import-jobs/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/settings/global").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/settings/global/energy-threshold-delegation").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, masterDataPaths).authenticated()
                        .requestMatchers(HttpMethod.POST, masterDataPaths).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, masterDataPaths).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, masterDataPaths).hasRole("ADMIN")
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${werkpilot.security.allowed-origin}") String allowedOrigin,
            @Value("${werkpilot.security.csrf-header-name}") String csrfHeaderName) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", csrfHeaderName));
        configuration.setExposedHeaders(List.of(csrfHeaderName));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
