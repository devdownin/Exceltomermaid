package com.example.excelgraph.service;

import com.example.excelgraph.model.GraphEdge;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelParserServiceTest {

    private final ExcelParserService excelParserService = new ExcelParserService();

    @Test
    void testParseExcelWithHeadersAndComponent() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Source");
            headerRow.createCell(1).setCellValue("Flux");
            headerRow.createCell(2).setCellValue("Destination");
            headerRow.createCell(3).setCellValue("Composant");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("App A");
            row1.createCell(1).setCellValue("HTTP REST");
            row1.createCell(2).setCellValue("App B");
            row1.createCell(3).setCellValue("API Gateway");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("App B");
            row2.createCell(1).setCellValue("Kafka Event");
            row2.createCell(2).setCellValue("App C");
            row2.createCell(3).setCellValue("Kafka Cluster");

            workbook.write(out);
        }

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        List<GraphEdge> edges = excelParserService.parseExcel(inputStream);

        assertEquals(2, edges.size());
        assertEquals("App A", edges.get(0).source());
        assertEquals("HTTP REST", edges.get(0).flux());
        assertEquals("App B", edges.get(0).destination());
        assertEquals("API Gateway", edges.get(0).component());

        assertEquals("App B", edges.get(1).source());
        assertEquals("Kafka Event", edges.get(1).flux());
        assertEquals("App C", edges.get(1).destination());
        assertEquals("Kafka Cluster", edges.get(1).component());
    }

    @Test
    void testParseExcelProcessusFormat() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Processus");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Processus");
            headerRow.createCell(1).setCellValue("Sous-processus");
            headerRow.createCell(2).setCellValue("Activité");
            headerRow.createCell(3).setCellValue("Applications");
            headerRow.createCell(4).setCellValue("Composants");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Gestion Ventes");
            row1.createCell(1).setCellValue("Prise Commande");
            row1.createCell(2).setCellValue("Saisie Panier");
            row1.createCell(3).setCellValue("E-Commerce Web");
            row1.createCell(4).setCellValue("Angular Frontend");

            workbook.write(out);
        }

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        List<GraphEdge> edges = excelParserService.parseExcel(inputStream);

        assertEquals(4, edges.size());

        // 1. Processus -> Sous-processus
        assertEquals("Gestion Ventes", edges.get(0).source());
        assertEquals("Sous-processus", edges.get(0).flux());
        assertEquals("Prise Commande", edges.get(0).destination());

        // 2. Sous-processus -> Activité
        assertEquals("Prise Commande", edges.get(1).source());
        assertEquals("Exécute", edges.get(1).flux());
        assertEquals("Saisie Panier", edges.get(1).destination());

        // 3. Activité -> Application
        assertEquals("Saisie Panier", edges.get(2).source());
        assertEquals("Utilise", edges.get(2).flux());
        assertEquals("E-Commerce Web", edges.get(2).destination());

        // 4. Application -> Composant
        assertEquals("E-Commerce Web", edges.get(3).source());
        assertEquals("Composé de", edges.get(3).flux());
        assertEquals("Angular Frontend", edges.get(3).destination());
    }

    @Test
    void testParseSampleFlowsFromClasspath() throws Exception {
        byte[] bytes;
        try (InputStream is = getClass().getResourceAsStream("/sample-flows.xlsx")) {
            assertNotNull(is, "sample-flows.xlsx should be available on classpath");
            bytes = is.readAllBytes();
        }

        List<GraphEdge> edgesSheet0 = excelParserService.parseExcel(new ByteArrayInputStream(bytes), 0);
        assertEquals(6, edgesSheet0.size());
        assertEquals("E-Commerce Web", edgesSheet0.get(0).source());
        assertEquals("Passation commande", edgesSheet0.get(0).flux());
        assertEquals("Order Service", edgesSheet0.get(0).destination());
        assertEquals("REST API", edgesSheet0.get(0).component());

        List<GraphEdge> procEdgesSheet1 = excelParserService.parseExcel(new ByteArrayInputStream(bytes), 1);
        assertFalse(procEdgesSheet1.isEmpty(), "Second sheet should parse Processus format");
    }

    @Test
    void testParseExcelWithEmptyDestinationAndExterneColumns() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Source");
            headerRow.createCell(1).setCellValue("Flux");
            headerRow.createCell(2).setCellValue("Destination");
            headerRow.createCell(3).setCellValue("Composant");
            headerRow.createCell(4).setCellValue("Externe");
            headerRow.createCell(5).setCellValue("Externe(2)");
            headerRow.createCell(6).setCellValue("Externe(3)");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("App Primary");
            row1.createCell(1).setCellValue("Sync Data");
            row1.createCell(2).setCellValue(""); // Empty Destination
            row1.createCell(3).setCellValue("FTP");
            row1.createCell(4).setCellValue("Partner Ext 1");
            row1.createCell(5).setCellValue("Partner Ext 2");
            row1.createCell(6).setCellValue("Partner Ext 3");

            workbook.write(out);
        }

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        List<GraphEdge> edges = excelParserService.parseExcel(inputStream);

        assertEquals(3, edges.size());
        assertEquals("App Primary", edges.get(0).source());
        assertEquals("Sync Data", edges.get(0).flux());
        assertEquals("Partner Ext 1", edges.get(0).destination());
        assertEquals("FTP", edges.get(0).component());

        assertEquals("Partner Ext 2", edges.get(1).destination());
        assertEquals("Partner Ext 3", edges.get(2).destination());
    }

    @Test
    void testParseExcelWithoutExplicitHeadersFallback() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            // Header with generic names
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Col1");
            headerRow.createCell(1).setCellValue("Col2");
            headerRow.createCell(2).setCellValue("Col3");
            headerRow.createCell(3).setCellValue("Col4");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Client");
            row1.createCell(1).setCellValue("gRPC");
            row1.createCell(2).setCellValue("Server");
            row1.createCell(3).setCellValue("Protobuf");

            workbook.write(out);
        }

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        List<GraphEdge> edges = excelParserService.parseExcel(inputStream);

        assertEquals(1, edges.size());
        assertEquals("Client", edges.get(0).source());
        assertEquals("gRPC", edges.get(0).flux());
        assertEquals("Server", edges.get(0).destination());
        assertEquals("Protobuf", edges.get(0).component());
    }
}
