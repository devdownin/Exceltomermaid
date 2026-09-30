package com.example.excelgraph.service;

import com.example.excelgraph.model.GraphEdge;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class ExcelParserService {

    public List<GraphEdge> parseExcel(InputStream inputStream) throws Exception {
        List<GraphEdge> edges = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return edges;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return edges;
            }

            // Find header row indices for source, flux, destination, composant
            Row headerRow = rowIterator.next();
            int sourceCol = -1;
            int fluxCol = -1;
            int destCol = -1;
            int compCol = -1;

            for (Cell cell : headerRow) {
                String val = getCellValueAsString(cell).toLowerCase(Locale.ROOT).trim();
                if (val.contains("source") || val.contains("src") || val.contains("origine")) {
                    sourceCol = cell.getColumnIndex();
                } else if (val.contains("flux") || val.contains("label") || val.contains("libelle") || val.contains("flow") || val.contains("nom")) {
                    fluxCol = cell.getColumnIndex();
                } else if (val.contains("dest") || val.contains("cible") || val.contains("target")) {
                    destCol = cell.getColumnIndex();
                } else if (val.contains("comp") || val.contains("techno") || val.contains("proto") || val.contains("outil") || val.contains("moyen")) {
                    compCol = cell.getColumnIndex();
                }
            }

            // Fallback if headers are not explicitly named: use cols 0, 1, 2, 3
            if (sourceCol == -1) sourceCol = 0;
            if (fluxCol == -1) fluxCol = 1;
            if (destCol == -1) destCol = 2;
            if (compCol == -1) compCol = 3;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row == null) continue;

                String source = getCellValueAsString(row.getCell(sourceCol));
                String flux = getCellValueAsString(row.getCell(fluxCol));
                String destination = getCellValueAsString(row.getCell(destCol));
                String component = getCellValueAsString(row.getCell(compCol));

                if (!source.isEmpty() || !destination.isEmpty()) {
                    edges.add(new GraphEdge(source, flux, destination, component));
                }
            }
        }
        return edges;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}
