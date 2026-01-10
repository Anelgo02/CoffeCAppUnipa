package com.example.coffecappunipa.web.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {
        "/route/login",
        "/route/register",
        "/route/logout"
})
public class RoutingServlet extends HttpServlet {

    // Chiavi legacy: restano qui solo perché usate altrove (bridge/servlet legacy)
    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_ROLE = "role";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/login")) {
            handleRouteLogin(req, resp);
            return;
        }
        if (uri.endsWith("/register")) {
            resp.sendRedirect("/cliente/registrazione.html");
            return;
        }
        if (uri.endsWith("/logout")) {
            // logout vero gestito da Spring Security (è POST)
            resp.sendRedirect("/auth/logout");
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // se qualcuno chiama POST /route/logout o /route/login, li trattiamo uguale
        doGet(req, resp);
    }

    private void handleRouteLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Se Spring Security non ti ha autenticato, vai al login statico
        if (req.getUserPrincipal() == null) {
            resp.sendRedirect("/login.html");
            return;
        }

        // Sei autenticato: redirect in base al ruolo Spring
        if (req.isUserInRole("MANAGER")) {
            resp.sendRedirect("/gestore/index.html");
            return;
        }
        if (req.isUserInRole("MAINTAINER")) {
            resp.sendRedirect("/manutenzione/index.html");
            return;
        }
        if (req.isUserInRole("CUSTOMER")) {
            resp.sendRedirect("/cliente/index.html");
            return;
        }

        resp.sendRedirect("/login.html?err=invalid_role");
    }
}
