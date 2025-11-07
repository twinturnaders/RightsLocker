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
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATHS = new AntPathMatcher();

    private final JwtService jwtService;
    private final AppUserRepo userRepo;

    // Only truly public paths here
    private final List<String> skip = List.of(
            "/api/auth/**",
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

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7).trim();
                try {
                    String sub = jwtService.validateAndGetSubject(token); // userId (UUID) as subject
                    var userOpt = userRepo.findById(UUID.fromString(sub));
                    if (userOpt.isPresent()) {
                        AppUser user = userOpt.get();
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                        // PRINCIPAL = AppUser so @AuthenticationPrincipal AppUser works
                        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception ignored) { /* invalid token -> anonymous */ }
            }
        }

        filterChain.doFilter(request, response);
    }
}
