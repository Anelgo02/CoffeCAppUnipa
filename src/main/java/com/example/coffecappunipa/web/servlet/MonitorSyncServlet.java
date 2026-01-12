// src/main/java/com/example/coffecappunipa/web/servlet/MonitorSyncServlet.java
package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.web.monitor.MonitorClient;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/monitor/sync"})
public class MonitorSyncServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        // Autorizzazione
        if (!isManagerOrMaintainer(req)) {
            resp.setStatus(403);
            resp.getWriter().write("{\"ok\":false,\"message\":\"ruolo non autorizzato\"}");
            return;
        }

        // 1) PULL dal Monitor
        Map<String, String> monitorStatuses = MonitorClient.fetchRuntimeStatuses();

        if (monitorStatuses == null || monitorStatuses.isEmpty()) {
            // monitor giù / endpoint errato / JSON non parseabile
            resp.setStatus(502);
            resp.getWriter().write("{\"ok\":false,\"message\":\"monitor non raggiungibile o risposta vuota\"}");
            return;
        }

        // 2) Apply sul DB principale
        try {
            DistributorDAO.SyncResult r = distributorDAO.applyStatusesFromMonitor(monitorStatuses);

            resp.setStatus(200);
            resp.getWriter().write("{"
                    + "\"ok\":true,"
                    + "\"received\":" + monitorStatuses.size() + ","
                    + "\"updated\":" + r.updated + ","
                    + "\"missing\":" + r.missing + ","
                    + "\"invalid\":" + r.invalid
                    + "}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private boolean isManagerOrMaintainer(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object role = s.getAttribute(RoutingServlet.SESSION_ROLE);
        if (role == null) return false;
        String r = role.toString().trim().toUpperCase();
        return "MANAGER".equals(r) || "MAINTAINER".equals(r);
    }
}
