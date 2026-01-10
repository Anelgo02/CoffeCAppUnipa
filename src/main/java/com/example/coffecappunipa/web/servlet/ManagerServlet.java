package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorAdminDAO;
import com.example.coffecappunipa.persistence.dao.MaintainerDAO;
import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;
import com.example.coffecappunipa.web.monitor.MonitorClient;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
        "/api/manager/maintainers.xml",
        "/api/manager/maintainers/list",
        "/api/manager/maintainers/create",
        "/api/manager/maintainers/delete",
        "/api/manager/distributors/list",
        "/api/manager/distributors/create",
        "/api/manager/distributors/delete",
        "/api/manager/distributors/status"
})
public class ManagerServlet extends HttpServlet {

    private final MaintainerDAO maintainerDAO = new MaintainerDAO();
    private final DistributorAdminDAO distributorAdminDAO = new DistributorAdminDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isManager(req)) {
            writeJson(resp, 403, "{\"ok\":false,\"message\":\"ruolo non autorizzato\"}");
            return;
        }

        String uri = req.getRequestURI();

        if (uri.endsWith("/maintainers.xml")) { handleMaintainersXml(resp); return; }
        if (uri.endsWith("/maintainers/list")) { handleMaintainersList(resp); return; }
        if (uri.endsWith("/distributors/list")) { handleDistributorsList(req, resp); return; }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isManager(req)) {
            writeJson(resp, 403, "{\"ok\":false,\"message\":\"ruolo non autorizzato\"}");
            return;
        }

        String uri = req.getRequestURI();

        if (uri.endsWith("/maintainers/create")) { handleCreateMaintainer(req, resp); return; }
        if (uri.endsWith("/maintainers/delete")) { handleDeleteMaintainer(req, resp); return; }

        if (uri.endsWith("/distributors/create")) { handleCreateDistributor(req, resp); return; }
        if (uri.endsWith("/distributors/delete")) { handleDeleteDistributor(req, resp); return; }
        if (uri.endsWith("/distributors/status")) { handleUpdateDistributorStatus(req, resp); return; }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleMaintainersXml(HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/xml");
        resp.setHeader("Cache-Control", "no-store");

        try {
            List<MaintainerDAO.MaintainerRow> list = maintainerDAO.findAllWithProfile();

            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
            xml.append("<manutentori>\n");

            for (var m : list) {
                String idXml = toXmlMaintainerId(m.maintainerId);
                xml.append("    <manutentore id=\"").append(escXml(idXml)).append("\">\n");
                xml.append("        <nome>").append(escXml(m.firstName)).append("</nome>\n");
                xml.append("        <cognome>").append(escXml(m.lastName)).append("</cognome>\n");
                xml.append("        <email>").append(escXml(m.email)).append("</email>\n");
                xml.append("        <telefono>").append(escXml(m.phone)).append("</telefono>\n");
                xml.append("    </manutentore>\n");
            }

            xml.append("</manutentori>");

            resp.setStatus(200);
            resp.getWriter().write(xml.toString());

        } catch (DaoException ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("<?xml version=\"1.0\" encoding=\"utf-8\" ?><error>db_error</error>");
        }
    }

    private String toXmlMaintainerId(String username) {
        if (username == null) return "";
        return username.replace("-", "").replace(" ", "");
    }

    private void handleMaintainersList(HttpServletResponse resp) throws IOException {
        try {
            List<MaintainerDAO.MaintainerRow> list = maintainerDAO.findAllWithProfile();

            StringBuilder json = new StringBuilder();
            json.append("{\"ok\":true,\"items\":[");
            for (int i = 0; i < list.size(); i++) {
                var m = list.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                        .append("\"id\":\"").append(escJson(m.maintainerId)).append("\",")
                        .append("\"nome\":\"").append(escJson(m.firstName)).append("\",")
                        .append("\"cognome\":\"").append(escJson(m.lastName)).append("\",")
                        .append("\"email\":\"").append(escJson(m.email)).append("\",")
                        .append("\"telefono\":\"").append(escJson(m.phone)).append("\"")
                        .append("}");
            }
            json.append("]}");

            writeJson(resp, 200, json.toString());

        } catch (DaoException ex) {
            ex.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleCreateMaintainer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = trim(req.getParameter("id"));
        String nome = trim(req.getParameter("nome"));
        String cognome = trim(req.getParameter("cognome"));
        String email = trim(req.getParameter("email"));
        String telefono = trim(req.getParameter("telefono"));

        if (isBlank(id) || isBlank(nome) || isBlank(cognome) || isBlank(email) || isBlank(telefono)) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"tutti i campi sono obbligatori\"}");
            return;
        }

        try {
            if (maintainerDAO.findUserIdByUsernameMaintainer(id).isPresent()) {
                writeJson(resp, 409, "{\"ok\":false,\"message\":\"manutentore già esistente\"}");
                return;
            }

            long userId = maintainerDAO.createMaintainer(id, nome, cognome, email, telefono);
            writeJson(resp, 201, "{\"ok\":true,\"userId\":" + userId + "}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleDeleteMaintainer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = trim(req.getParameter("id"));
        if (isBlank(id)) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"id obbligatorio\"}");
            return;
        }

        try {
            maintainerDAO.deleteMaintainerById(id);
            writeJson(resp, 200, "{\"ok\":true}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleDistributorsList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String qRaw = trim(req.getParameter("q"));

        Map<String, String> monitorStatuses = MonitorClient.fetchRuntimeStatuses();

        String base =
                "SELECT code, location_name, status " +
                        "FROM distributors ";

        boolean hasQ = !isBlank(qRaw);
        String qNorm = hasQ ? qRaw.trim().toUpperCase() : null;

        String qAsDbStatus = null;
        if (hasQ) {
            qAsDbStatus = DistributorAdminDAO.uiStatusToDbEnum(qNorm);

            if (qAsDbStatus == null) {
                if ("ACTIVE".equals(qNorm) || "MAINTENANCE".equals(qNorm) || "FAULT".equals(qNorm)) {
                    qAsDbStatus = qNorm;
                }
                if ("GUASTO".equals(qNorm)) {
                    qAsDbStatus = "FAULT";
                }
            }
        }

        String where = "";
        if (hasQ) {
            if (qAsDbStatus != null) {
                where = "WHERE code LIKE ? OR location_name LIKE ? OR status = ? ";
            } else {
                where = "WHERE code LIKE ? OR location_name LIKE ? OR status LIKE ? ";
            }
        }

        String sql = base + where + "ORDER BY code";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasQ) {
                String like = "%" + qRaw + "%";
                ps.setString(1, like);
                ps.setString(2, like);

                if (qAsDbStatus != null) {
                    ps.setString(3, qAsDbStatus);
                } else {
                    ps.setString(3, "%" + qNorm + "%");
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder json = new StringBuilder();
                json.append("{\"ok\":true,\"items\":[");
                boolean first = true;

                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    String code = rs.getString("code");
                    String loc = rs.getString("location_name");
                    String dbStatus = rs.getString("status");

                    String runtime = monitorStatuses.get(code);
                    String ui = mergedDbToUi(dbStatus, runtime);

                    json.append("{")
                            .append("\"id\":\"").append(escJson(code)).append("\",")
                            .append("\"luogo\":\"").append(escJson(loc)).append("\",")
                            .append("\"stato\":\"").append(escJson(ui)).append("\"")
                            .append("}");
                }

                json.append("]}");
                writeJson(resp, 200, json.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private String mergedDbToUi(String dbStatus, String monitorStatusDb) {
        String db = (dbStatus == null) ? "" : dbStatus.trim().toUpperCase();
        String mon = (monitorStatusDb == null) ? "" : monitorStatusDb.trim().toUpperCase();

        // DB in MAINTENANCE è “bloccante”: prevale sempre
        if ("MAINTENANCE".equals(db)) return "MANUTENZIONE";

        // Se il monitor dice FAULT, e non sei in manutenzione, mostra GUASTO (runtime > DB)
        if ("FAULT".equals(mon) && !"MAINTENANCE".equals(db)) return "GUASTO";

        if ("ACTIVE".equals(db)) return "ATTIVO";
        if ("FAULT".equals(db)) return "GUASTO";
        if ("MAINTENANCE".equals(db)) return "MANUTENZIONE";

        return "DISATTIVO";
    }

    private void handleCreateDistributor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = trim(req.getParameter("id"));
        String loc = trim(req.getParameter("locazione"));
        String statoUi = trim(req.getParameter("stato"));

        if (isBlank(id) || isBlank(loc) || isBlank(statoUi)) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"id, locazione, stato obbligatori\"}");
            return;
        }

        String statusEnum = DistributorAdminDAO.uiStatusToDbEnum(statoUi);
        if (statusEnum == null) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"stato non valido\"}");
            return;
        }

        try {
            long distId = distributorAdminDAO.createDistributorWithSupplies(id, loc, statusEnum);

            // Allinea il monitor al DB (best-effort)
            MonitorClient.upsertDistributor(id, loc, statusEnum);

            // 🔥 FIX: heartbeat SOLO se lo stato è ACTIVE
            if (isActive(statusEnum)) {
                MonitorClient.heartbeat(id);
            }

            writeJson(resp, 201, "{\"ok\":true,\"distributorId\":" + distId + "}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleDeleteDistributor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = trim(req.getParameter("id"));
        if (isBlank(id)) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"id obbligatorio\"}");
            return;
        }

        try {
            distributorAdminDAO.deleteDistributorByCode(id);
            MonitorClient.deleteDistributor(id);
            writeJson(resp, 200, "{\"ok\":true}");
        } catch (DaoException ex) {
            ex.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private void handleUpdateDistributorStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = trim(req.getParameter("id"));
        String statoUi = trim(req.getParameter("stato"));

        if (isBlank(id) || isBlank(statoUi)) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"id e stato obbligatori\"}");
            return;
        }

        String statusEnum = DistributorAdminDAO.uiStatusToDbEnum(statoUi);
        if (statusEnum == null) {
            writeJson(resp, 400, "{\"ok\":false,\"message\":\"stato non valido\"}");
            return;
        }

        try {
            // 1) DB principale
            distributorAdminDAO.updateStatusByCode(id, statusEnum);

            // 2) Monitor (best-effort): aggiorna lo stato
            MonitorClient.updateStatus(id, statusEnum);

            // 🔥 FIX: se imposto ACTIVE, devo “ravvivare” il runtime del monitor
            if (isActive(statusEnum)) {
                MonitorClient.heartbeat(id);
            }

            writeJson(resp, 200, "{\"ok\":true}");

        } catch (DaoException ex) {
            ex.printStackTrace();
            writeJson(resp, 500, "{\"ok\":false,\"message\":\"errore DB\"}");
        }
    }

    private boolean isActive(String statusEnum) {
        return statusEnum != null && "ACTIVE".equalsIgnoreCase(statusEnum.trim());
    }

    private boolean isManager(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object role = s.getAttribute(RoutingServlet.SESSION_ROLE);
        return role != null && "MANAGER".equalsIgnoreCase(role.toString());
    }

    private void writeJson(HttpServletResponse resp, int status, String payload) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
        resp.setStatus(status);
        resp.getWriter().write(payload);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
