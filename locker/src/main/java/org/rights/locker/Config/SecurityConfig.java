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
                        // auth (public)
                        .requestMatchers("/api/auth/**").permitAll()

                        // anonymous upload only these 2
                        .requestMatchers("/api/evidence/presign-upload", "/api/evidence/finalize").permitAll()

                        // public share links
                        .requestMatchers("/api/share/**").permitAll()

                        // health/docs/static
                        .requestMatchers("/actuator/**", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // everything else requires JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
