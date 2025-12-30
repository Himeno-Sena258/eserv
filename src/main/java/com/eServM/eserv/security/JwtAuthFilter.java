package com.eServM.eserv.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if (!path.startsWith("/api")) {
            return true;
        }
        if (path.startsWith("/api/login") || path.startsWith("/api/register")) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            unauthorized(response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            if (!jwtService.validateToken(token)) {
                unauthorized(response);
                return;
            }
            Claims claims = jwtService.parseClaims(token);
            String role = claims.get("role", String.class);
            String username = claims.getSubject();
            request.setAttribute("currentRole", role);
            request.setAttribute("currentUsername", username);
        } catch (Exception ex) {
            unauthorized(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"无效的令牌\"}");
    }

}
