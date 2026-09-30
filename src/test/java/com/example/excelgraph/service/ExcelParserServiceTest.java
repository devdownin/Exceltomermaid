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
    void testParseExcelWithHeaders() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Source");
            headerRow.createCell(1).setCellValue("Flux");
            headerRow.createCell(2).setCellValue("Destination");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("App A");
            row1.createCell(1).setCellValue("HTTP REST");
            row1.createCell(2).setCellValue("App B");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("App B");
            row2.createCell(1).setCellValue("Kafka Event");
            row2.createCell(2).setCellValue("App C");

            workbook.write(out);
        }

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        List<GraphEdge> edges = excelParserService.parseExcel(inputStream);

        assertEquals(2, edges.size());
        assertEquals("App A", edges.get(0).source());
        assertEquals("HTTP REST", edges.get(0).flux());
        assertEquals("App B", edges.get(0).destination());

        assertEquals("App B", edges.get(1).source());
        assertEquals("Kafka Event", edges.get(1).flux());
        assertEquals("App C", edges.get(1).destination());
    }

    @Test
    void testParseSampleFlowsFromClasspath() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/sample-flows.xlsx")) {
            assertNotNull(is, "sample-flows.xlsx should be available on classpath");
            List<GraphEdge> edges = excelParserService.parseExcel(is);

            assertEquals(6, edges.size());
            assertEquals("E-Commerce Web", edges.get(0).source());
            assertEquals("REST /orders", edges.get(0).flux());
            assertEquals("Order Service", edges.get(0).destination());

            assertEquals("Inventory Service", edges.get(5).source());
            assertEquals("SQL Query", edges.get(5).flux());
            assertEquals("PostgreSQL Database", edges.get(5).destination());
        }
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

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Client");
            row1.createCell(1).setCellValue("gRPC");
            row1.createCell(2).setCellValue("Server");

            workbook.write(out);
        }

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        List<GraphEdge> edges = excelParserService.parseExcel(inputStream);

        assertEquals(1, edges.size());
        assertEquals("Client", edges.get(0).source());
        assertEquals("gRPC", edges.get(0).flux());
        assertEquals("Server", edges.get(0).destination());
    }
}
