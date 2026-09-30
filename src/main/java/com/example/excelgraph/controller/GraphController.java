package com.example.excelgraph.controller;

import com.example.excelgraph.model.GraphEdge;
import com.example.excelgraph.model.GraphResponse;
import com.example.excelgraph.service.ExcelParserService;
import com.example.excelgraph.service.GraphGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class GraphController {

    private final ExcelParserService excelParserService;
    private final GraphGeneratorService graphGeneratorService;

    public GraphController(ExcelParserService excelParserService, GraphGeneratorService graphGeneratorService) {
        this.excelParserService = excelParserService;
        this.graphGeneratorService = graphGeneratorService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("orientation", "LR");
        return "index";
    }

    @PostMapping("/upload")
    public String uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "orientation", defaultValue = "LR") String orientation,
            Model model) {
        model.addAttribute("orientation", orientation);

        if (file.isEmpty()) {
            model.addAttribute("error", "Veuillez sélectionner un fichier Excel valide.");
            return "index";
        }

        try {
            List<GraphEdge> edges = excelParserService.parseExcel(file.getInputStream());
            GraphResponse response = graphGeneratorService.generateGraphResponse(edges, orientation);

            model.addAttribute("graphData", response);
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("edgeCount", edges.size());
            model.addAttribute("nodeCount", response.nodes().size());
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la lecture du fichier Excel: " + e.getMessage());
        }

        return "index";
    }

    @PostMapping("/api/graph/upload")
    @ResponseBody
    public ResponseEntity<?> apiUploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "orientation", defaultValue = "LR") String orientation) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichier vide ou absent.");
        }

        try {
            List<GraphEdge> edges = excelParserService.parseExcel(file.getInputStream());
            GraphResponse response = graphGeneratorService.generateGraphResponse(edges, orientation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'analyse: " + e.getMessage());
        }
    }
}
