package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.web.monitor.MonitorClient;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = "/api/distributor/boot")
public class DistributorBootServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");

        String code = req.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Codice distributore mancante\"}");
            return;
        }

        // 1. Verifica Esistenza nel DB
        Long id = distributorDAO.findIdByCode(code);

        if (id == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Distributore non registrato dal Manager\"}");
            return;
        }

        // 2. Notifica al Monitor che ci siamo accesi
        // Manda un heartbeat immediato per dire "Sono ONLINE"
        try {
            MonitorClient.heartbeat(code);
        } catch (Exception ignored) {}

        resp.setStatus(200);
        resp.getWriter().write("{\"ok\":true,\"message\":\"Boot completato\"}");
    }
}