package org.rights.locker.Config;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // auth (public)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("api/evidence/thumb").permitAll()
                        .requestMatchers("/apiv/share/**", "/api/share/**").permitAll()

                        // anonymous upload only these 2
                        .requestMatchers("/api/evidence/presign-upload", "/api/evidence/finalize", "/api/evidence/thumb/**").permitAll()
                        .requestMatchers("/api/evidence/share/**", "/api/evidence/share/**").permitAll()
                        // everything else under /api/evidence requires auth
                        .requestMatchers(HttpMethod.GET, "/api/evidence", "/api/evidence/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/evidence/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/evidence/**").authenticated()

                        // public share links


                        // health/docs/static
                        .requestMatchers("/actuator/**", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // any other endpoint requires auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(false);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
