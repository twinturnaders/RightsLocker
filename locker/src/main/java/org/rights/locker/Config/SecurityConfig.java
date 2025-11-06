package org.rights.locker.Config;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Security.JwtAuthenticationFilter;
import org.rights.locker.Security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                // CORS + CSRF
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                // Absolutely stateless: every request must carry its own auth
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Exception handling -> consistent JSON for 401/403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(json401())
                        .accessDeniedHandler(json403())
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()

                        // Anonymous upload / convert flow
                        .requestMatchers(HttpMethod.POST, "/api/evidence/presign-upload").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/evidence/finalize").permitAll()

                        // Public share capability links
                        .requestMatchers("/api/share/**").permitAll()
                        .requestMatchers(
                                "/api/evidence/presign-upload",
                                "/api/evidence/finalize",
                                "/api/evidence/*/package",
                                "/api/evidence/thumb",
                                "/api/share/**",
                                "/actuator/health").permitAll()
                        // Public health/docs (optional)
                        .requestMatchers("/actuator/health", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Everything else needs a valid JWT
                        .anyRequest().authenticated()
                )

                // Our JWT filter runs before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Nice JSON 401
    private AuthenticationEntryPoint json401() {
        return (request, response, authException) -> writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    // Nice JSON 403
    private AccessDeniedHandler json403() {
        return (request, response, accessDeniedException) -> writeError(response, HttpStatus.FORBIDDEN, "Forbidden");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status.value() + ",\"error\":\"" + message + "\"}");
    }

    // CORS for SPA; tweak origins as needed
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of(
                "https://rightslocker.org",
                "https://www.rightslocker.org",
                "http://localhost:4200",
                "http://127.0.0.1:8080",
                "http://127.0.0.1:30080",
                "http://127.0.0.1:30082"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        cfg.setAllowCredentials(false);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}