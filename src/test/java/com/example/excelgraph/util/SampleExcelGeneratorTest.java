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
            header.createCell(3).setCellValue("Composant");

            String[][] rows = {
                {"E-Commerce Web", "Passation commande", "Order Service", "REST API"},
                {"Order Service", "Publication événement", "Inventory Service", "Kafka Topic"},
                {"Order Service", "Facturation", "Billing Service", "Kafka Topic"},
                {"Order Service", "Envoi notification", "Notification Service", "gRPC"},
                {"Billing Service", "Traitement paiement", "Payment Gateway", "HTTPS / Spring Web Client"},
                {"Inventory Service", "Lecture stock", "PostgreSQL Database", "JDBC Driver"}
            };

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(rows[i][0]);
                row.createCell(1).setCellValue(rows[i][1]);
                row.createCell(2).setCellValue(rows[i][2]);
                row.createCell(3).setCellValue(rows[i][3]);
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
