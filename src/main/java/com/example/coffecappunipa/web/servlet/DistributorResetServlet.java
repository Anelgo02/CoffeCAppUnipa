package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.persistence.util.DaoException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = "/api/distributor/reset")
public class DistributorResetServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Deve arrivare autenticato dal DistributorTokenFilter
        if (auth == null || !auth.isAuthenticated()
                || auth.getAuthorities().stream().noneMatch(a -> "ROLE_DISTRIBUTOR".equals(a.getAuthority()))) {
            resp.setStatus(401);
            resp.getWriter().write("{\"ok\":false,\"message\":\"unauthorized\"}");
            return;
        }

        // Il principal è il codice distributore (es. UNIPA-001) impostato dal filtro
        String distributorCode = String.valueOf(auth.getPrincipal());

        try {
            // Imposta token a NULL nel DB
            distributorDAO.updateSecurityToken(distributorCode, null);

            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":true,\"message\":\"reset_ok\"}");
        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Errore interno del server\"}");
        }
    }
}
