package com.itasocialacademy.oitassist.security.config;

import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import com.itasocialacademy.oitassist.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.itasocialacademy.oitassist.security.oauth2.OAuth2FailureHandler;
import com.itasocialacademy.oitassist.security.oauth2.OAuth2SuccessHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final AuthenticationEntryPoint entryPoint;
    private final OAuth2SuccessHandler successHandler;
    private final OAuth2FailureHandler failureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    public SecurityConfig(
        JwtFilter jwtFilter,
        AuthenticationEntryPoint entryPoint,
        OAuth2SuccessHandler successHandler,
        OAuth2FailureHandler failureHandler,
        HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        this.jwtFilter = jwtFilter;
        this.entryPoint = entryPoint;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://*:5173",
            "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/registration/**",
                    "/api/v1/security/signIn",
                    "/api/v1/security/refresh",
                    "/api/v1/security/2fa/verify",
                    "/api/v1/security/2fa/enroll",
                    "/api/v1/security/2fa/enroll/confirm",
                    "/api/v1/user-activation/resend")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/news/**",
                    "/api/v1/user-activation/verify",
                    "/ui",
                    "/ui/**",
                    "/uploads/news/**",
                    "/api/v1/version")
                .permitAll()
                .requestMatchers(
                    "/index.html",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/oauth-test.html",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/docs/**",
                    "/docs")
                .permitAll()
                .anyRequest()
                .authenticated())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(entryPoint))
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                .successHandler(successHandler)
                .failureHandler(failureHandler))
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}