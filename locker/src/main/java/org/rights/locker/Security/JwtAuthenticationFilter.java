package org.rights.locker.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Services.CurrentUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtService jwtService;
    private final AppUser userPrincipal;
    private final CurrentUserService currentUserService;
    private final List<String> skipPatterns = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/health",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );



    public JwtAuthenticationFilter(JwtService jwtService, CurrentUserService currentUserService) {
        this.jwtService = jwtService;
        this.userPrincipal = userPrincipal;

        this.currentUserService = currentUserService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return skipPatterns.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Already authenticated? Move on.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract Bearer token
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7).trim();

        // Validate & parse
        String userId;
        try {
            userId = jwtService.parseSubject(token);  // throws on invalid/expired
        } catch (Exception e) {
            filterChain.doFilter(request, response);  // silently skip → endpoint can still be public
            return;
        }

       UserPrincipal userPrincipal = UserPrincipal.create(currentUserService.getCurrentUser(userId));
        var authToken = new UsernamePasswordAuthenticationToken(
                userPrincipal.getAuthorities(),
                userPrincipal.getPassword()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
