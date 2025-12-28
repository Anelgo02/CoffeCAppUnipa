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

@WebServlet(urlPatterns = {
        "/api/maintainer/distributors/refill",
        "/api/maintainer/distributors/status"
})
public class MaintainerDistributorsServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        if (!isMaintainer(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"ok\":false,\"message\":\"ruolo non autorizzato\"}");
            return;
        }

        String uri = req.getRequestURI();

        if (uri.endsWith("/refill")) { handleRefill(req, resp); return; }
        if (uri.endsWith("/status")) { handleStatus(req, resp); return; }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleRefill(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = req.getParameter("code");
        if (isBlank(code)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"code obbligatorio\"}");
            return;
        }

        int coffee = 2000;
        int milk = 5;
        int sugar = 2000;
        int cups = 200;

        try {
            distributorDAO.refillSuppliesByCode(code.trim(), coffee, milk, sugar, cups);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"ok\":true}");
        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = req.getParameter("code");
        String statusXml = req.getParameter("status"); // attivo/manutenzione/disattivo

        if (isBlank(code) || isBlank(statusXml)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"code e status obbligatori\"}");
            return;
        }

        String dbStatus = toDbStatus(statusXml.trim().toLowerCase());
        if (dbStatus == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"status non valido\"}");
            return;
        }

        try {
            distributorDAO.updateStatusByCode(code.trim(), dbStatus);

            // BEST-EFFORT: aggiorna anche il monitor (no auth)
            MonitorClient.updateStatus(code.trim(), dbStatus);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"ok\":true}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private String toDbStatus(String xmlStatus) {
        return switch (xmlStatus) {
            case "attivo" -> "ACTIVE";
            case "manutenzione" -> "MAINTENANCE";
            case "disattivo" -> "FAULT";
            default -> null;
        };
    }

    private boolean isMaintainer(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object role = s.getAttribute(RoutingServlet.SESSION_ROLE);
        return role != null && "MAINTAINER".equalsIgnoreCase(role.toString());
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
