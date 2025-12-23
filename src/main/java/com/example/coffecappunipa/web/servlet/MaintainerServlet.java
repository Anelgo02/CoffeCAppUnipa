package com.example.coffecappunipa.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {
        "/api/maintainer/me"
})

public class MaintainerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        //controllo la sessione corrente
        HttpSession session = req.getSession(false);
        if(session == null){
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Sessione non valida\"}");
            return;
        }

        Object u = session.getAttribute(RoutingServlet.SESSION_USERNAME);
        Object r = session.getAttribute(RoutingServlet.SESSION_ROLE);

        //controllo
        if(u == null || r == null){
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Sessione non valida\"}");
            return;
        }

        String role = r.toString();
        //qui non facciamo AUTH ma almeno blocchiamo accessi casuali alla pagina manutentore
        if(!"MAINTAINER".equalsIgnoreCase(role)){
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"ok\":false,\"message\":\"Ruolo non autorizzato\"}");
            return;
        }

        String username = u.toString();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"ok\":true,\"username\":\"" + escape(username) + "\", \"role\":\"MAINTAINER\"}");
    }

    private String escape(String s){
        if(s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
