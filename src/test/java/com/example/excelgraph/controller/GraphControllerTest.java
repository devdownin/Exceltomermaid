package com.example.excelgraph.controller;

import com.example.excelgraph.service.ExcelParserService;
import com.example.excelgraph.service.GraphGeneratorService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraphController.class)
@Import({ExcelParserService.class, GraphGeneratorService.class})
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testUploadExcelFile() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Source");
            header.createCell(1).setCellValue("Flux");
            header.createCell(2).setCellValue("Destination");
            header.createCell(3).setCellValue("Composant");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("System 1");
            row.createCell(1).setCellValue("API Call");
            row.createCell(2).setCellValue("System 2");
            row.createCell(3).setCellValue("REST Client");

            workbook.write(out);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("graphData"))
                .andExpect(model().attribute("edgeCount", 1))
                .andExpect(model().attribute("nodeCount", 2));
    }

    @Test
    void testUploadExcelFileWithIncludeExternalFalse() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Source");
            header.createCell(1).setCellValue("Flux");
            header.createCell(2).setCellValue("Destination");
            header.createCell(3).setCellValue("Composant");
            header.createCell(4).setCellValue("Externe");

            // Row 1: Regular destination
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("System 1");
            row1.createCell(1).setCellValue("API Call");
            row1.createCell(2).setCellValue("System 2");
            row1.createCell(3).setCellValue("REST Client");

            // Row 2: External destination (empty destination)
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("System 1");
            row2.createCell(1).setCellValue("Ext Call");
            row2.createCell(2).setCellValue("");
            row2.createCell(3).setCellValue("SFTP");
            row2.createCell(4).setCellValue("Partner Ext");

            workbook.write(out);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

        mockMvc.perform(multipart("/upload").file(file).param("includeExternal", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("graphData"))
                .andExpect(model().attribute("edgeCount", 1))
                .andExpect(model().attribute("includeExternal", false));
    }

    @Test
    void testApiUploadExcelFile() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Source");
            header.createCell(1).setCellValue("Flux");
            header.createCell(2).setCellValue("Destination");
            header.createCell(3).setCellValue("Composant");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Service A");
            row.createCell(1).setCellValue("Kafka");
            row.createCell(2).setCellValue("Service B");
            row.createCell(3).setCellValue("Spring Cloud Stream");

            workbook.write(out);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

        mockMvc.perform(multipart("/api/graph/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edges[0].source").value("Service A"))
                .andExpect(jsonPath("$.edges[0].flux").value("Kafka"))
                .andExpect(jsonPath("$.edges[0].destination").value("Service B"))
                .andExpect(jsonPath("$.edges[0].component").value("Spring Cloud Stream"));
    }
}
