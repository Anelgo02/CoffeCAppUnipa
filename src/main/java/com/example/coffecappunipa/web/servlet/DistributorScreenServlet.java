package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.BeverageDAO;
import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.persistence.dao.DistributorScreenDAO;
import com.example.coffecappunipa.persistence.util.DaoException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {
        "/api/distributor/poll",
        "/api/distributor/beverages",
        "/api/distributor/purchase"
})
public class DistributorScreenServlet extends HttpServlet {

    private final DistributorScreenDAO screenDAO = new DistributorScreenDAO();
    private final BeverageDAO beverageDAO = new BeverageDAO();
    private final DistributorDAO distributorDAO = new DistributorDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupJson(resp);

        String uri = req.getRequestURI();
        if (uri.endsWith("/poll")) { handlePoll(req, resp); return; }
        if (uri.endsWith("/beverages")) { handleBeverages(resp); return; }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupJson(resp);

        if (req.getRequestURI().endsWith("/purchase")) {
            handlePurchase(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handlePoll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = trim(req.getParameter("code"));
        if (isBlank(code)) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"message\":\"code obbligatorio\"}");
            return;
        }

        try {

            String status = distributorDAO.findStatusByCode(code);
            if(status==null){
                resp.setStatus(400);
                resp.getWriter().write("{\"ok\":false,\"message\":\"code obbligatorio\"}");
                return;
            }

            var opt = screenDAO.findConnectedCustomerByDistributorCode(code);

            // Costruiamo il JSON
            StringBuilder json = new StringBuilder();
            json.append("{")
                    .append("\"ok\":true,")
                    .append("\"status\":\"").append(status).append("\",");

            if (opt.isEmpty()) {
                json.append("\"connected\":false");
            } else {
                var c = opt.get();
                json.append("\"connected\":true,")
                        .append("\"customerId\":").append(c.customerId).append(",")
                        .append("\"username\":\"").append(escJson(c.username)).append("\",")
                        .append("\"credit\":").append(c.credit == null ? "0.00" : c.credit.toPlainString());
            }
            json.append("}");

            resp.setStatus(200);
            resp.getWriter().write(json.toString());

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleBeverages(HttpServletResponse resp) throws IOException {
        try {
            var list = beverageDAO.findActive();

            StringBuilder json = new StringBuilder();
            json.append("{\"ok\":true,\"items\":[");
            for (int i = 0; i < list.size(); i++) {
                var b = list.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                        .append("\"id\":").append(b.id).append(",")
                        .append("\"name\":\"").append(escJson(b.name)).append("\",")
                        .append("\"price\":").append(b.price == null ? "0.00" : b.price.toPlainString())
                        .append("}");
            }
            json.append("]}");

            resp.setStatus(200);
            resp.getWriter().write(json.toString());

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handlePurchase(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = trim(req.getParameter("code"));
        String bevIdStr = trim(req.getParameter("beverageId"));
        String sugarStr = trim(req.getParameter("sugarQty"));

        if (isBlank(code) || isBlank(bevIdStr)) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"message\":\"code e beverageId obbligatori\"}");
            return;
        }

        long bevId;
        try {
            bevId = Long.parseLong(bevIdStr);
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"message\":\"beverageId non valido\"}");
            return;
        }

        int sugarQty = 0;
        if (!isBlank(sugarStr)) {
            try { sugarQty = Integer.parseInt(sugarStr); }
            catch (Exception ignore) { sugarQty = 0; }
        }
        if (sugarQty < 0) sugarQty = 0;
        if (sugarQty > 10) sugarQty = 10; // limite per non rompere scorte con input assurdi

        try {
            // NB: prezzo e validazione bevanda avvengono nel DAO in transazione
            var newCredit = screenDAO.performPurchase(code, bevId, sugarQty);

            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":true,\"credit\":" + newCredit.toPlainString() + "}");

        } catch (DaoException ex) {
            String err = ex.getMessage();

            if (DistributorScreenDAO.ERR_NO_CUSTOMER_CONNECTED.equals(err)) {
                resp.setStatus(409);
                resp.getWriter().write("{\"ok\":false,\"message\":\"nessun cliente connesso\"}");
                return;
            }
            if (DistributorScreenDAO.ERR_INVALID_BEVERAGE.equals(err)) {
                resp.setStatus(404);
                resp.getWriter().write("{\"ok\":false,\"message\":\"bevanda non trovata\"}");
                return;
            }
            if (DistributorScreenDAO.ERR_INVALID_PRICE.equals(err)) {
                resp.setStatus(409);
                resp.getWriter().write("{\"ok\":false,\"message\":\"prezzo non valido\"}");
                return;
            }
            if (DistributorScreenDAO.ERR_INSUFFICIENT_CREDIT.equals(err)) {
                resp.setStatus(409);
                resp.getWriter().write("{\"ok\":false,\"message\":\"credito insufficiente\"}");
                return;
            }
            if (DistributorScreenDAO.ERR_OUT_OF_STOCK.equals(err)) {
                resp.setStatus(409);
                resp.getWriter().write("{\"ok\":false,\"message\":\"scorte insufficienti\"}");
                return;
            }

            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void setupJson(HttpServletResponse resp) {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
