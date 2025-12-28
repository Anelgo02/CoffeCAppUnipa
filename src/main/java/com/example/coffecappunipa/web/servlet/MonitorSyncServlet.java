package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.ManagerReadDAO;
import com.example.coffecappunipa.web.monitor.MonitorClient;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/api/monitor/sync"})
public class MonitorSyncServlet extends HttpServlet {

    private final ManagerReadDAO managerReadDAO = new ManagerReadDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        if (!isManagerOrMaintainer(req)) {
            resp.setStatus(403);
            resp.getWriter().write("{\"ok\":false,\"message\":\"ruolo non autorizzato\"}");
            return;
        }

        // recupero dal DB principale: [code, location_name, status]
        List<String[]> list = managerReadDAO.distributorsList();

        // creo JSON minimo per /api/monitor/sync
        StringBuilder json = new StringBuilder();
        json.append("{\"items\":[");
        for (int i = 0; i < list.size(); i++) {
            String[] r = list.get(i);
            String code = r[0];
            String loc = r[1];
            String st = r[2]; // già ACTIVE/MAINTENANCE/FAULT

            if (i > 0) json.append(",");
            json.append("{")
                    .append("\"code\":\"").append(esc(code)).append("\",")
                    .append("\"location_name\":\"").append(esc(loc)).append("\",")
                    .append("\"status\":\"").append(esc(st)).append("\"")
                    .append("}");
        }
        json.append("]}");

        // invio best-effort
        MonitorClient.syncJson(json.toString());

        resp.setStatus(200);
        resp.getWriter().write("{\"ok\":true,\"count\":" + list.size() + "}");
    }

    private boolean isManagerOrMaintainer(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object role = s.getAttribute(RoutingServlet.SESSION_ROLE);
        if (role == null) return false;
        String r = role.toString().trim().toUpperCase();
        return "MANAGER".equals(r) || "MAINTAINER".equals(r);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
