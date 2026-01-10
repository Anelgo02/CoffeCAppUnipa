package com.example.coffecappunipa.web.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@WebServlet(urlPatterns = {"/api/monitor/map"})
public class MonitorMapProxyServlet extends HttpServlet {

    // URL REALE (corretto) del monitor
    private static final String MONITOR_MAP_URL =
            "http://localhost:8081/CoffeeMonitor_war_exploded/api/monitor/map";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MONITOR_MAP_URL))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> r = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            resp.setStatus(r.statusCode());
            resp.getWriter().write(r.body());

        } catch (Exception e) {
            // Degrado controllato: monitor giù
            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":false,\"items\":[],\"message\":\"monitor_unreachable\"}");
        }
    }
}
