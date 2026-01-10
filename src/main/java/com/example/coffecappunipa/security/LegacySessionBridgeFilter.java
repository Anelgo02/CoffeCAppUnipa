package com.example.coffecappunipa.security;

import com.example.coffecappunipa.web.servlet.RoutingServlet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class LegacySessionBridgeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal())) {

            HttpSession session = request.getSession(true);

            // usa ESATTAMENTE le chiavi legacy:
            session.setAttribute(RoutingServlet.SESSION_USERNAME, auth.getName());

            String role = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(r -> r.startsWith("ROLE_"))
                    .map(r -> r.substring("ROLE_".length()))
                    .findFirst()
                    .orElse(null);

            if (role != null) {
                session.setAttribute(RoutingServlet.SESSION_ROLE, role);
            }
        }

        filterChain.doFilter(request, response);
    }
}
