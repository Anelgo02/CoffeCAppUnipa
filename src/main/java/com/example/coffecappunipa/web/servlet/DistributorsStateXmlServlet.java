package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;
import com.example.coffecappunipa.web.monitor.MonitorClient;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@WebServlet(urlPatterns = "/api/distributors/state.xml")
public class DistributorsStateXmlServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/xml");
        resp.setHeader("Cache-Control", "no-store");

        Map<String, String> monitorStatuses = MonitorClient.fetchRuntimeStatuses();

        var list = distributorDAO.findAllStatesForXml();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<stato_generale_distributori xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("                             xsi:noNamespaceSchemaLocation=\"stato_distributori.xsd\">\n\n");

        for (var d : list) {
            String code = d.getCode();
            String dbStatus = d.getStatus();

            String runtime = monitorStatuses.get(code);
            boolean runtimeFault = "FAULT".equalsIgnoreCase(runtime);

            String xmlOperational;
            if ("MAINTENANCE".equalsIgnoreCase(dbStatus)) {
                xmlOperational = "manutenzione";
            } else if (runtimeFault) {
                xmlOperational = "disattivo";
            } else {
                xmlOperational = toXmlOperationalStatus(dbStatus);
            }

            xml.append("    <distributore id=\"").append(escXml(code)).append("\">\n");
            xml.append("        <locazione>").append(escXml(d.getLocationName())).append("</locazione>\n");
            xml.append("        <stato_operativo>").append(escXml(xmlOperational)).append("</stato_operativo>\n");
            xml.append("        <livelli_forniture>\n");

            xml.append("            <caffe_gr>").append(d.getCoffeeLevel()).append("</caffe_gr>\n");
            xml.append("            <latte_lt>").append(d.getMilkLevel()).append(".0</latte_lt>\n");
            xml.append("            <cioccolata_gr>").append(0).append("</cioccolata_gr>\n");
            xml.append("            <te_gr>").append(0).append("</te_gr>\n");
            xml.append("            <zucchero_gr>").append(d.getSugarLevel()).append("</zucchero_gr>\n");
            xml.append("            <bicchieri_num>").append(d.getCupsLevel()).append("</bicchieri_num>\n");

            xml.append("        </livelli_forniture>\n");

            boolean hasDbFaults = d.getFaults() != null && !d.getFaults().isEmpty();
            boolean injectHeartbeatFault = runtimeFault && !hasDbFaults && !"MAINTENANCE".equalsIgnoreCase(dbStatus);

            xml.append("        <guasti>\n");

            if (hasDbFaults) {
                for (var f : d.getFaults()) {
                    xml.append("            <guasto>\n");
                    xml.append("                <codice>").append(escXml(f.getCode())).append("</codice>\n");
                    xml.append("                <descrizione>").append(escXml(f.getDescription())).append("</descrizione>\n");

                    String dt = (f.getCreatedAt() == null)
                            ? LocalDateTime.now().format(ISO)
                            : f.getCreatedAt().format(ISO);

                    xml.append("                <data_rilevazione>").append(escXml(dt)).append("</data_rilevazione>\n");
                    xml.append("            </guasto>\n");
                }
            }

            if (injectHeartbeatFault) {
                String now = LocalDateTime.now().format(ISO);
                xml.append("            <guasto>\n");
                xml.append("                <codice>").append("HB-FAULT").append("</codice>\n");
                xml.append("                <descrizione>").append(escXml("Heartbeat assente oltre la soglia (guasto rilevato dal monitor)")).append("</descrizione>\n");
                xml.append("                <data_rilevazione>").append(escXml(now)).append("</data_rilevazione>\n");
                xml.append("            </guasto>\n");
            }

            xml.append("        </guasti>\n");
            xml.append("    </distributore>\n\n");
        }

        xml.append("</stato_generale_distributori>\n");

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(xml.toString());
    }

    private String toXmlOperationalStatus(String dbStatus) {
        if (dbStatus == null) return "disattivo";
        String v = dbStatus.trim().toUpperCase();
        return switch (v) {
            case "ACTIVE" -> "attivo";
            case "MAINTENANCE" -> "manutenzione";
            case "FAULT" -> "disattivo";
            default -> "disattivo";
        };
    }

    private String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}