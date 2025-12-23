package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.ConnectionDAO;
import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.persistence.dao.UserDAO;
import com.example.coffecappunipa.persistence.util.DaoException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {
        "/api/customer/connect",
        "/api/customer/disconnect",
        "/api/customer/current-connection"
})
public class CustomerConnectionServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final DistributorDAO distributorDAO = new DistributorDAO();
    private final ConnectionDAO connectionDAO = new ConnectionDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/connect")) {
            handleConnect(req, resp);
            return;
        }
        if (uri.endsWith("/disconnect")) {
            handleDisconnect(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getRequestURI().endsWith("/current-connection")) {
            handleCurrent(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleConnect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupJson(resp);

        String username = getLoggedUsername(req);
        if (username == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"ok\":false,\"message\":\"sessione non valida\"}");
            return;
        }

        String code = req.getParameter("code");
        if (isBlank(code)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"code obbligatorio\"}");
            return;
        }

        try {
            var userOpt = userDAO.findByUsername(username);
            if (userOpt.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"ok\":false,\"message\":\"utente non trovato\"}");
                return;
            }

            Long distributorId = distributorDAO.findIdByCode(code);
            if (distributorId == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"ok\":false,\"message\":\"distributore non trovato\"}");
                return;
            }

            connectionDAO.connect(userOpt.get().getId(), distributorId);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"ok\":true}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleDisconnect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupJson(resp);

        String username = getLoggedUsername(req);
        if (username == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"ok\":false,\"message\":\"sessione non valida\"}");
            return;
        }

        try {
            var userOpt = userDAO.findByUsername(username);
            if (userOpt.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"ok\":false,\"message\":\"utente non trovato\"}");
                return;
            }

            connectionDAO.disconnect(userOpt.get().getId());
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"ok\":true}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleCurrent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupJson(resp);

        String username = getLoggedUsername(req);
        if (username == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"ok\":false,\"message\":\"sessione non valida\"}");
            return;
        }

        try {
            var userOpt = userDAO.findByUsername(username);
            if (userOpt.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"ok\":false,\"message\":\"utente non trovato\"}");
                return;
            }

            Long distId = connectionDAO.findActiveDistributorId(userOpt.get().getId());

            resp.setStatus(HttpServletResponse.SC_OK);
            if (distId == null) {
                resp.getWriter().write("{\"ok\":true,\"connected\":false}");
            } else {
                resp.getWriter().write("{\"ok\":true,\"connected\":true,\"distributorId\":" + distId + "}");
            }

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private String getLoggedUsername(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object u = s.getAttribute(RoutingServlet.SESSION_USERNAME);
        return u == null ? null : u.toString();
    }

    private void setupJson(HttpServletResponse resp) {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
