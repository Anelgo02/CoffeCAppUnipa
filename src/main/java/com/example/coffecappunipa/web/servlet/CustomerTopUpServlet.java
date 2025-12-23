package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.UserDAO;
import com.example.coffecappunipa.persistence.util.DaoException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = "/api/customer/topup")
public class CustomerTopUpServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        String username = getLoggedUsername(req);
        if (username == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"ok\":false,\"message\":\"sessione non valida\"}");
            return;
        }

        String amountStr = req.getParameter("amount");
        if (amountStr == null || amountStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"amount obbligatorio\"}");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"amount non valido\"}");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"ok\":false,\"message\":\"amount deve essere > 0\"}");
            return;
        }

        try {
            var opt = userDAO.findByUsername(username);
            if (opt.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"ok\":false,\"message\":\"utente non trovato\"}");
                return;
            }

            var user = opt.get();
            BigDecimal newCredit = userDAO.topUpCredit(user.getId(), amount);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"ok\":true,\"credit\":" + newCredit + "}");

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
}
