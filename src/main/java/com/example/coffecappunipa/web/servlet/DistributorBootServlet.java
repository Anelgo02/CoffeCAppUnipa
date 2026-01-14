package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.web.monitor.MonitorClient;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@WebServlet(urlPatterns = "/api/distributor/boot")
public class DistributorBootServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        String code = req.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Codice distributore mancante\"}");
            return;
        }

        code = code.trim();

        try {
            // 1) Verifica che esista
            Long id = distributorDAO.findIdByCode(code);
            if (id == null) {
                resp.setStatus(404);
                resp.getWriter().write("{\"ok\":false,\"message\":\"Distributore non registrato dal Manager\"}");
                return;
            }

            // 2) BLOCCO RE-BOOT: se c'è già un token, non rigenerare
            String existingToken = distributorDAO.findSecurityTokenByCode(code);
            if (existingToken != null && !existingToken.isBlank()) {
                resp.setStatus(409); // Conflict
                resp.getWriter().write("{\"ok\":false,\"message\":\"Distributore già inizializzato. Effettua Reset ID per reinizializzare.\"}");
                return;
            }

            // 3) Genera token
            String token = UUID.randomUUID().toString();

            // 4) Salva token nel DB
            distributorDAO.updateSecurityToken(code, token);

            // 5) Notifica monitor (best effort)
            try {
                MonitorClient.heartbeat(code);
            } catch (Exception ignored) { }

            // 6) Risposta OK
            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":true,\"message\":\"Boot completato\",\"token\":\"" + token + "\"}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Errore interno del server\"}");
        }
    }
}
