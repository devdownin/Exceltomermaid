package com.example.excelgraph.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;

public class SampleExcelGeneratorTest {

    @Test
    void generateSampleExcelFiles() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Flux_Applicatifs");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Source");
            header.createCell(1).setCellValue("Flux");
            header.createCell(2).setCellValue("Destination");

            String[][] rows = {
                {"E-Commerce Web", "REST /orders", "Order Service"},
                {"Order Service", "Kafka OrderCreated", "Inventory Service"},
                {"Order Service", "Kafka OrderCreated", "Billing Service"},
                {"Order Service", "gRPC", "Notification Service"},
                {"Billing Service", "HTTPS /payment", "Payment Gateway"},
                {"Inventory Service", "SQL Query", "PostgreSQL Database"}
            };

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(rows[i][0]);
                row.createCell(1).setCellValue(rows[i][1]);
                row.createCell(2).setCellValue(rows[i][2]);
            }

            try (FileOutputStream out = new FileOutputStream("src/test/resources/sample-flows.xlsx")) {
                workbook.write(out);
            }
            try (FileOutputStream out = new FileOutputStream("src/main/resources/static/sample-flows.xlsx")) {
                workbook.write(out);
            }
        }
    }
}
