package org.rights.locker.Config;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Security.JwtAuthenticationFilter;
import org.rights.locker.Security.JwtService;
import org.rights.locker.Repos.AppUserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Create the filter as a bean to avoid circulars and make it visible to the chain
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, AppUserRepo userRepo) {
        return new JwtAuthenticationFilter(jwtService, userRepo);
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()

                        // anonymous upload/convert
                        .requestMatchers("/api/evidence/presign-upload", "/api/evidence/finalize").permitAll()

                        // share capability links (public)
                        .requestMatchers("/api/share/**").permitAll()

                        // health/docs/static
                        .requestMatchers("/actuator/**", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // everything else requires auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    // wide-open CORS for now; tighten as you like
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*")); // or your exact frontend origin(s)
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
