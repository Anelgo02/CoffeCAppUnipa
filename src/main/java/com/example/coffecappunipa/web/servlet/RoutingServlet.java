package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.UserDAO;
import com.example.coffecappunipa.persistence.util.DaoException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(urlPatterns = {
        "/route/login",
        "/route/register",
        "/route/logout"
})
public class RoutingServlet extends HttpServlet {

    // session keys (non è “autenticazione”, è solo stato di navigazione)
    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_ROLE = "role";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/login")) {
            handleRouteLogin(req, resp);
            return;
        }
        if (uri.endsWith("/register")) {
            handleRegisterCustomer(req, resp);
            return;
        }
        if (uri.endsWith("/logout")) {
            handleLogout(req, resp);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // comodo per logout via link
        if (req.getRequestURI().endsWith("/logout")) {
            handleLogout(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * “Login” NON autenticato: prende solo username,
     * verifica esistenza e indirizza alla home corretta per ruolo.
     */
    private void handleRouteLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        if (isBlank(username)) {
            resp.sendRedirect("/login.html?err=missing_username");
            return;
        }

        try {
            var userOpt = userDAO.findByUsername(username);
            if (userOpt.isEmpty()) {
                // non esiste -> rimando al login o alla registrazione cliente
                resp.sendRedirect("/login.html?err=not_found");
                return;
            }

            var user = userOpt.get();

            // memorizzo in sessione per comandi successivi (connect, topup, ecc.)
            HttpSession session = req.getSession(true);
            session.setAttribute(SESSION_USERNAME, user.getUsername());
            session.setAttribute(SESSION_ROLE, user.getRole());

            // redirect in base al ruolo
            resp.sendRedirect(targetForRole(user.getRole()));

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.sendRedirect("/login.html?err=server");
        }
    }

    /**
     * Registrazione (solo customer): crea utente CUSTOMER se non esiste.
     */
    private void handleRegisterCustomer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");

        if (isBlank(username)) {
            resp.sendRedirect("/cliente/registrazione.html?err=missing_username");
            return;
        }

        try {
            if (userDAO.findByUsername(username).isPresent()) {
                resp.sendRedirect("/cliente/registrazione.html?err=already_exists");
                return;
            }

            userDAO.createCustomer(username, email);

            HttpSession session = req.getSession(true);
            session.setAttribute(SESSION_USERNAME, username);
            session.setAttribute(SESSION_ROLE, "CUSTOMER");

            resp.sendRedirect("/cliente/index.html");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.sendRedirect("/cliente/registrazione.html?err=server");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        resp.sendRedirect("/login.html");
    }

    private String targetForRole(String role) {
        if (role == null) return "/login.html?err=invalid_role";

        if ("CUSTOMER".equalsIgnoreCase(role)) return "/cliente/index.html";
        if ("MAINTAINER".equalsIgnoreCase(role)) return "/manutenzione/index.html";
        if ("MANAGER".equalsIgnoreCase(role)) return "/gestore/index.html";

        return "/login.html?err=invalid_role";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}