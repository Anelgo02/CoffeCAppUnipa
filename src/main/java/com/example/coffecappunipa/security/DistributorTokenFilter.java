package com.example.coffecappunipa.security;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro custom per l'autenticazione IoT.
 * Trasforma il Token nell'Header in una identità Spring Security.
 */
public class DistributorTokenFilter extends OncePerRequestFilter {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Cerca il token nell'header della richiesta
        String token = request.getHeader("X-Distributor-Auth");

        // 2. Se c'è un token e l'utente non è già autenticato (es. non è un cliente loggato)
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. Verifica nel DB se il token esiste
            String distributorCode = distributorDAO.findCodeBySecurityToken(token);

            if (distributorCode != null) {
                // 4. CREA L'IDENTITÀ (Authentication)
                // Creiamo un utente "virtuale" con ruolo ROLE_DISTRIBUTOR
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        distributorCode, // Il Principal sarà il codice (es. UNIPA-001)
                        null,            // Nessuna password
                        List.of(new SimpleGrantedAuthority("ROLE_DISTRIBUTOR")) // Assegna il ruolo
                );

                // 5. Inserisce l'identità nel contesto di Spring Security
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 6. Passa la palla al prossimo filtro
        filterChain.doFilter(request, response);
    }
}