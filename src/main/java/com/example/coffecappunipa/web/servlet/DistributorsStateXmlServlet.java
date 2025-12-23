package com.example.coffecappunipa.web.servlet;

import com.example.coffecappunipa.persistence.dao.DistributorDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@WebServlet(urlPatterns = "/api/distributors/state.xml")
public class DistributorsStateXmlServlet extends HttpServlet {

    private final DistributorDAO distributorDAO = new DistributorDAO();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/xml");
        resp.setHeader("Cache-Control", "no-store");

        var list = distributorDAO.findAllStatesForXml();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<stato_generale_distributori xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("                             xsi:noNamespaceSchemaLocation=\"stato_distributori.xsd\">\n\n");

        for (var d : list) {
            xml.append("    <distributore id=\"").append(escXml(d.getCode())).append("\">\n");
            xml.append("        <locazione>").append(escXml(d.getLocationName())).append("</locazione>\n");
            xml.append("        <stato_operativo>").append(escXml(toXmlOperationalStatus(d.getStatus()))).append("</stato_operativo>\n");
            xml.append("        <livelli_forniture>\n");

            // Mapping su DB reale
            xml.append("            <caffe_gr>").append(d.getCoffeeLevel()).append("</caffe_gr>\n");

            // Il tuo XML vuole un double: stampo "X.0"
            xml.append("            <latte_lt>").append(d.getMilkLevel()).append(".0</latte_lt>\n");

            // Non presenti nel DB → placeholder 0 (coerente e stabile)
            xml.append("            <cioccolata_gr>").append(0).append("</cioccolata_gr>\n");
            xml.append("            <te_gr>").append(0).append("</te_gr>\n");

            xml.append("            <zucchero_gr>").append(d.getSugarLevel()).append("</zucchero_gr>\n");
            xml.append("            <bicchieri_num>").append(d.getCupsLevel()).append("</bicchieri_num>\n");

            xml.append("        </livelli_forniture>\n");

            if (d.getFaults().isEmpty()) {
                xml.append("        <guasti/>\n");
            } else {
                xml.append("        <guasti>\n");
                for (var f : d.getFaults()) {
                    xml.append("            <guasto>\n");
                    xml.append("                <codice>").append(escXml(f.getCode())).append("</codice>\n");
                    xml.append("                <descrizione>").append(escXml(f.getDescription())).append("</descrizione>\n");

                    String dt = (f.getCreatedAt() == null) ? "" : f.getCreatedAt().format(ISO);
                    xml.append("                <data_rilevazione>").append(escXml(dt)).append("</data_rilevazione>\n");
                    xml.append("            </guasto>\n");
                }
                xml.append("        </guasti>\n");
            }

            xml.append("    </distributore>\n\n");
        }

        xml.append("</stato_generale_distributori>\n");

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(xml.toString());
    }

    /**
     * DB: ACTIVE / MAINTENANCE / FAULT
     * XML richiesto: attivo / manutenzione / disattivo
     */
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
