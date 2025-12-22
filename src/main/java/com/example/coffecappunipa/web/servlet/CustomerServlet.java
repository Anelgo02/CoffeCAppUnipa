package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.UserDAO;
import com.example.coffecappunipa.persistence.util.DaoException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {
        "/api/customer/register",
        "/api/customer/get"
})
public class CustomerServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // POST /api/customer/register
        if (req.getRequestURI().endsWith("/register")) {
            handleRegister(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // GET /api/customer/get?username=...
        if (req.getRequestURI().endsWith("/get")) {
            handleGet(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");

        String username = req.getParameter("username");
        String email = req.getParameter("email");

        if (isBlank(username)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"username obbligatorio\"}");
            return;
        }

        try {
            // se esiste già → errore
            if (userDAO.findByUsername(username).isPresent()) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"ok\":false,\"message\":\"username già esistente\"}");
                return;
            }

            long newId = userDAO.createCustomer(username, email);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("{\"ok\":true,\"id\":" + newId + "}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"DB non disponibile o errore JDBC\"}");

        }
    }

    private void handleGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");

        String username = req.getParameter("username");
        if (isBlank(username)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"username obbligatorio\"}");
            return;
        }

        try {
            var opt = userDAO.findByUsername(username);
            if (opt.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"ok\":false,\"message\":\"utente non trovato\"}");
                return;
            }

            var u = opt.get();
            // JSON semplice (senza librerie)
            String json = "{"
                    + "\"ok\":true,"
                    + "\"id\":" + u.getId() + ","
                    + "\"username\":\"" + escape(u.getUsername()) + "\","
                    + "\"email\":\"" + escape(u.getEmail()) + "\","
                    + "\"role\":\"" + escape(u.getRole()) + "\","
                    + "\"credit\":\"" + u.getCredit() + "\""
                    + "}";

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json);

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"DB non disponibile o errore JDBC\"}");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}