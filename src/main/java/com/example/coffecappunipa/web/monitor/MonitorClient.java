package com.example.coffecappunipa.web.monitor;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonitorClient {

    private static final String BASE =
            env("MONITOR_BASE_URL", "http://localhost:8081/CoffeeMonitor_war_exploded/api/monitor");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    public static void heartbeat(String code) {
        postForm("/heartbeat", "code", code);
    }

    public static void upsertDistributor(String code, String locationName, String statusDb) {
        postForm("/distributors/create",
                "code", code,
                "location_name", locationName == null ? "" : locationName,
                "status", statusDb);
    }

    public static void deleteDistributor(String code) {
        postForm("/distributors/delete", "code", code);
    }

    public static void updateStatus(String code, String statusDb) {
        postForm("/distributors/status", "code", code, "status", statusDb);
    }

    public static Map<String, String> fetchRuntimeStatuses() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + "/map"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) return Map.of();

            String body = res.body();
            if (body == null || body.isBlank()) return Map.of();

            return parseMapJson(body);

        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static Map<String, String> parseMapJson(String json) {
        Map<String, String> out = new HashMap<>();
        Pattern p = Pattern.compile("\"code\"\\s*:\\s*\"([^\"]*)\"[\\s\\S]*?\"status\"\\s*:\\s*\"([^\"]*)\"", Pattern.MULTILINE);
        Matcher m = p.matcher(json);

        while (m.find()) {
            String code = unescapeJson(m.group(1));
            String status = unescapeJson(m.group(2));
            if (code != null && !code.isBlank() && status != null && !status.isBlank()) {
                out.put(code.trim(), status.trim().toUpperCase());
            }
        }
        return out;
    }

    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public static void syncJson(String json) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + "/sync"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            client.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private static void postForm(String path, String... kv) {
        try {
            String body = formEncode(kv);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            client.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private static String formEncode(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append("&");
            sb.append(enc(kv[i])).append("=").append(enc(kv[i + 1] == null ? "" : kv[i + 1]));
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}