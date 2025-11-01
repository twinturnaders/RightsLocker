package org.rights.locker.Config;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Security.JwtAuthenticationFilter;
import org.rights.locker.Security.JwtService;
import org.rights.locker.Services.CurrentUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .cors(c -> {}) // configure a CorsConfigurationSource bean if needed
                .csrf(csrf -> csrf.disable()) // stateless API with Authorization header
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/evidence/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/evidence/presign-upload",
                                "/api/evidence/finalize").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/evidence/**/download").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthFilter(JwtService jwtService, CurrentUserService currentUserService) {
        return new JwtAuthenticationFilter(jwtService, currentUserService);
    }
    @Bean
    PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception { return cfg.getAuthenticationManager(); }
    @Bean

        CorsConfigurationSource corsConfigurationSource() {
            var cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(List.of(
                    "https://rightslocker.org",
                    "https://www.rightslocker.org",
                    "http://localhost:4200"
            ));
            cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            cfg.setAllowedHeaders(List.of("*"));
            cfg.setAllowCredentials(true);
            var src = new UrlBasedCorsConfigurationSource();
            src.registerCorsConfiguration("/**", cfg);
            return src;
        }
    }

