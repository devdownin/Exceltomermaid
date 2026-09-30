package com.example.excelgraph.service;

import com.example.excelgraph.model.GraphEdge;
import com.example.excelgraph.model.GraphResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphGeneratorServiceTest {

    private final GraphGeneratorService generatorService = new GraphGeneratorService();

    @Test
    void testGenerateGraphResponseFlowchartSyntax() {
        List<GraphEdge> edges = List.of(
            new GraphEdge("Sys A", "FTP", "Sys B"),
            new GraphEdge("Sys B", "REST", "Sys C")
        );

        GraphResponse response = generatorService.generateGraphResponse(edges, "LR");

        assertNotNull(response);
        assertEquals(2, response.edges().size());
        assertEquals(3, response.nodes().size());
        assertTrue(response.nodes().contains("Sys A"));
        assertTrue(response.nodes().contains("Sys B"));
        assertTrue(response.nodes().contains("Sys C"));

        // Check Mermaid flowchart formatting
        assertTrue(response.mermaidCode().contains("flowchart LR"));
        assertTrue(response.mermaidCode().contains("N1[\"Sys A\"]"));
        assertTrue(response.mermaidCode().contains("N2[\"Sys B\"]"));
        assertTrue(response.mermaidCode().contains("N3[\"Sys C\"]"));
        assertTrue(response.mermaidCode().contains("N1 -->|\"FTP\"| N2"));
        assertTrue(response.mermaidCode().contains("N2 -->|\"REST\"| N3"));

        // Check DOT formatting
        assertTrue(response.dotCode().contains("digraph G {"));
        assertTrue(response.dotCode().contains("\"Sys A\" -> \"Sys B\" [label=\"FTP\"];"));
    }
}
