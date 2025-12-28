package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.web.monitor.MonitorClient;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/monitor/heartbeat"})
public class MonitorProxyServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        String code = req.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"message\":\"code obbligatorio\"}");
            return;
        }

        // inoltro best-effort al monitor
        MonitorClient.heartbeat(code.trim());

        resp.setStatus(200);
        resp.getWriter().write("{\"ok\":true}");
    }
}
