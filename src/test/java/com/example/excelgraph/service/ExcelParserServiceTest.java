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
    void testParseSampleFlowsFromClasspath() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/sample-flows.xlsx")) {
            assertNotNull(is, "sample-flows.xlsx should be available on classpath");
            List<GraphEdge> edges = excelParserService.parseExcel(is);

            assertEquals(6, edges.size());
            assertEquals("E-Commerce Web", edges.get(0).source());
            assertEquals("Passation commande", edges.get(0).flux());
            assertEquals("Order Service", edges.get(0).destination());
            assertEquals("REST API", edges.get(0).component());

            assertEquals("Inventory Service", edges.get(5).source());
            assertEquals("Lecture stock", edges.get(5).flux());
            assertEquals("PostgreSQL Database", edges.get(5).destination());
            assertEquals("JDBC Driver", edges.get(5).component());
        }
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
