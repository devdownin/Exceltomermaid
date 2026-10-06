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
            // Sheet 1: Flux_Applicatifs
            Sheet sheet1 = workbook.createSheet("Flux_Applicatifs");

            Row header1 = sheet1.createRow(0);
            header1.createCell(0).setCellValue("Source");
            header1.createCell(1).setCellValue("Flux");
            header1.createCell(2).setCellValue("Destination");
            header1.createCell(3).setCellValue("Composant");

            String[][] rows1 = {
                {"E-Commerce Web", "Passation commande", "Order Service", "REST API"},
                {"Order Service", "Publication événement", "Inventory Service", "Kafka Topic"},
                {"Order Service", "Facturation", "Billing Service", "Kafka Topic"},
                {"Order Service", "Envoi notification", "Notification Service", "gRPC"},
                {"Billing Service", "Traitement paiement", "Payment Gateway", "HTTPS / Spring Web Client"},
                {"Inventory Service", "Lecture stock", "PostgreSQL Database", "JDBC Driver"}
            };

            for (int i = 0; i < rows1.length; i++) {
                Row row = sheet1.createRow(i + 1);
                row.createCell(0).setCellValue(rows1[i][0]);
                row.createCell(1).setCellValue(rows1[i][1]);
                row.createCell(2).setCellValue(rows1[i][2]);
                row.createCell(3).setCellValue(rows1[i][3]);
            }

            // Sheet 2: Processus_et_Activites
            Sheet sheet2 = workbook.createSheet("Processus_et_Activités");

            Row header2 = sheet2.createRow(0);
            header2.createCell(0).setCellValue("Processus");
            header2.createCell(1).setCellValue("Sous-processus");
            header2.createCell(2).setCellValue("Activité");
            header2.createCell(3).setCellValue("Applications");
            header2.createCell(4).setCellValue("Composants");

            String[][] rows2 = {
                {"Ventes & Commandes", "Prise de commande", "Saisie panier", "E-Commerce Web", "Angular Frontend"},
                {"Ventes & Commandes", "Prise de commande", "Validation paiement", "Payment Gateway", "Stripe API"},
                {"Ventes & Commandes", "Gestion de stock", "Réservation article", "Inventory Service", "Spring Boot Service"},
                {"Ventes & Commandes", "Gestion de stock", "Mise à jour stock", "PostgreSQL Database", "SQL Database"},
                {"Support Client", "", "Traiter réclamation", "CRM Application", "Salesforce Module"}
            };

            for (int i = 0; i < rows2.length; i++) {
                Row row = sheet2.createRow(i + 1);
                for (int j = 0; j < rows2[i].length; j++) {
                    row.createCell(j).setCellValue(rows2[i][j]);
                }
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
