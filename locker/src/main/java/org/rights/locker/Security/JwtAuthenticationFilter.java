package org.rights.locker.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATHS = new AntPathMatcher();

    private final JwtService jwtService;
    private final AppUserRepo userRepo;

    private final List<String> skip = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/evidence/presign-upload",
            "/api/evidence/finalize",
            "/api/share/**",
            "/actuator/health",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return skip.stream().anyMatch(p -> PATHS.match(p, path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            try {
                String sub = jwtService.validateAndGetSubject(token); // validates signature & exp
                UUID userId = UUID.fromString(sub);
                AppUser user = userRepo.findById(userId).orElse(null);
                if (user != null) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    var principal = new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
                    var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                // invalid/expired/unknown user → leave context empty; protected routes will 401/403
            }
        }

        filterChain.doFilter(request, response);
    }
}