package com.example.excelgraph.service;

import com.example.excelgraph.model.GraphEdge;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class ExcelParserService {

    public List<String> getSheetNames(InputStream inputStream) throws Exception {
        List<String> names = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                names.add(workbook.getSheetName(i));
            }
        }
        return names;
    }

    public List<GraphEdge> parseExcel(InputStream inputStream) throws Exception {
        return parseExcel(inputStream, 0);
    }

    public List<GraphEdge> parseExcel(InputStream inputStream, int sheetIndex) throws Exception {
        Set<GraphEdge> uniqueEdges = new LinkedHashSet<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            int totalSheets = workbook.getNumberOfSheets();
            if (totalSheets == 0) return new ArrayList<>();

            if (sheetIndex < 0 || sheetIndex >= totalSheets) {
                sheetIndex = 0;
            }

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return new ArrayList<>();
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return new ArrayList<>();
            }

            Row headerRow = rowIterator.next();

            int procCol = -1;
            int subProcCol = -1;
            int actCol = -1;
            int appCol = -1;
            int compColProc = -1;

            int sourceCol = -1;
            int fluxCol = -1;
            int destCol = -1;
            int compColFlow = -1;
            List<Integer> externeCols = new ArrayList<>();

            for (Cell cell : headerRow) {
                String val = getCellValueAsString(cell).toLowerCase(Locale.ROOT).trim();

                // Check Processus format headers
                if (val.contains("sous") && (val.contains("proc") || val.contains("processus"))) {
                    subProcCol = cell.getColumnIndex();
                } else if (val.contains("processus") || val.contains("proc")) {
                    procCol = cell.getColumnIndex();
                } else if (val.contains("activit") || val.contains("action") || val.contains("tâche") || val.contains("tache")) {
                    actCol = cell.getColumnIndex();
                } else if (val.contains("application") || val.contains("appli") || val.contains("logiciel") || val.contains("système")) {
                    appCol = cell.getColumnIndex();
                }

                // Check Flow format headers
                if (val.contains("source") || val.contains("src") || val.contains("origine")) {
                    sourceCol = cell.getColumnIndex();
                } else if (val.contains("flux") || val.contains("label") || val.contains("libelle") || val.contains("flow")) {
                    fluxCol = cell.getColumnIndex();
                } else if (val.contains("dest") || val.contains("cible") || val.contains("target")) {
                    destCol = cell.getColumnIndex();
                } else if (val.contains("comp") || val.contains("techno") || val.contains("proto") || val.contains("outil")) {
                    compColFlow = cell.getColumnIndex();
                    compColProc = cell.getColumnIndex();
                } else if (val.contains("externe") && externeCols.size() < 15) {
                    externeCols.add(cell.getColumnIndex());
                }
            }

            boolean isProcessusFormat = (procCol != -1 || actCol != -1) && (appCol != -1 || subProcCol != -1 || actCol != -1);

            if (isProcessusFormat) {
                // Parse as Processus / Activités / Applications format
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (row == null) continue;

                    List<String> procs = splitCellValues(getSafeCellValue(row, procCol));
                    List<String> subProcs = splitCellValues(getSafeCellValue(row, subProcCol));
                    List<String> acts = splitCellValues(getSafeCellValue(row, actCol));
                    List<String> apps = splitCellValues(getSafeCellValue(row, appCol));
                    List<String> comps = splitCellValues(getSafeCellValue(row, compColProc));

                    // 1. Processus -> Sous-processus
                    for (String p : procs) {
                        for (String sp : subProcs) {
                            uniqueEdges.add(new GraphEdge(p, "Sous-processus", sp, "Processus", false));
                        }
                    }

                    // 2. Processus / Sous-processus -> Activité
                    for (String act : acts) {
                        if (!subProcs.isEmpty()) {
                            for (String sp : subProcs) {
                                uniqueEdges.add(new GraphEdge(sp, "Exécute", act, "Activité", false));
                            }
                        } else if (!procs.isEmpty()) {
                            for (String p : procs) {
                                uniqueEdges.add(new GraphEdge(p, "Exécute", act, "Activité", false));
                            }
                        }
                    }

                    // 3. Activité / Sous-processus / Processus -> Application
                    for (String app : apps) {
                        if (!acts.isEmpty()) {
                            for (String act : acts) {
                                uniqueEdges.add(new GraphEdge(act, "Utilise", app, "Application", false));
                            }
                        } else if (!subProcs.isEmpty()) {
                            for (String sp : subProcs) {
                                uniqueEdges.add(new GraphEdge(sp, "Utilise", app, "Application", false));
                            }
                        } else if (!procs.isEmpty()) {
                            for (String p : procs) {
                                uniqueEdges.add(new GraphEdge(p, "Utilise", app, "Application", false));
                            }
                        }
                    }

                    // 4. Application -> Composant
                    for (String app : apps) {
                        for (String comp : comps) {
                            uniqueEdges.add(new GraphEdge(app, "Composé de", comp, "Composant", false));
                        }
                    }
                }
            } else {
                // Parse as Flow format
                if (sourceCol == -1) sourceCol = 0;
                if (fluxCol == -1) fluxCol = 1;
                if (destCol == -1) destCol = 2;
                if (compColFlow == -1) compColFlow = 3;

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (row == null) continue;

                    String source = getSafeCellValue(row, sourceCol);
                    String flux = getSafeCellValue(row, fluxCol);
                    String destination = getSafeCellValue(row, destCol);
                    String component = getSafeCellValue(row, compColFlow);

                    if (!destination.trim().isEmpty()) {
                        if (!source.isEmpty() || !destination.isEmpty()) {
                            uniqueEdges.add(new GraphEdge(source, flux, destination, component, false));
                        }
                    } else {
                        boolean addedFromExterne = false;
                        for (int extCol : externeCols) {
                            String extDest = getSafeCellValue(row, extCol);
                            if (!extDest.trim().isEmpty()) {
                                uniqueEdges.add(new GraphEdge(source, flux, extDest, component, true));
                                addedFromExterne = true;
                            }
                        }
                        if (!addedFromExterne && (!source.isEmpty() || !flux.isEmpty())) {
                            uniqueEdges.add(new GraphEdge(source, flux, "", component, false));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(uniqueEdges);
    }

    private List<String> splitCellValues(String cellVal) {
        if (cellVal == null || cellVal.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = cellVal.split("[,;\n\r]+");
        List<String> list = new ArrayList<>();
        for (String t : tokens) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String getSafeCellValue(Row row, int colIndex) {
        if (row == null || colIndex < 0) return "";
        return getCellValueAsString(row.getCell(colIndex));
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}
