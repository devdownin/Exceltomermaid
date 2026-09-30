package com.example.excelgraph.service;

import com.example.excelgraph.model.GraphEdge;
import com.example.excelgraph.model.GraphResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphGeneratorService {

    public GraphResponse generateGraphResponse(List<GraphEdge> edges) {
        return generateGraphResponse(edges, "LR");
    }

    public GraphResponse generateGraphResponse(List<GraphEdge> edges, String orientation) {
        if (orientation == null || (!orientation.equalsIgnoreCase("TD") && !orientation.equalsIgnoreCase("TB"))) {
            orientation = "LR";
        } else {
            orientation = orientation.toUpperCase(Locale.ROOT);
        }

        Set<String> nodes = new LinkedHashSet<>();
        for (GraphEdge edge : edges) {
            if (!edge.source().isEmpty()) nodes.add(edge.source());
            if (!edge.destination().isEmpty()) nodes.add(edge.destination());
        }

        String mermaid = generateMermaidCode(edges, orientation);
        String dot = generateDotCode(edges, orientation);

        return new GraphResponse(edges, nodes, mermaid, dot);
    }

    private String generateMermaidCode(List<GraphEdge> edges, String orientation) {
        StringBuilder sb = new StringBuilder("graph ").append(orientation).append("\n");

        for (GraphEdge edge : edges) {
            String src = sanitize(edge.source());
            String dest = sanitize(edge.destination());
            String flux = sanitize(edge.flux());

            if (src.isEmpty() && dest.isEmpty()) continue;

            if (flux.isEmpty()) {
                sb.append(String.format("    \"%s\" --> \"%s\"\n", src, dest));
            } else {
                sb.append(String.format("    \"%s\" -- \"%s\" --> \"%s\"\n", src, flux, dest));
            }
        }

        // Add class definitions for flux styling
        sb.append("\n    %% Custom styling based on flux types\n");
        sb.append("    classDef rest fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;\n");
        sb.append("    classDef kafka fill:#fff3e0,stroke:#f57c00,stroke-width:2px;\n");
        sb.append("    classDef sql fill:#e8f5e9,stroke:#388e3c,stroke-width:2px;\n");

        return sb.toString();
    }

    private String generateDotCode(List<GraphEdge> edges, String orientation) {
        StringBuilder sb = new StringBuilder("digraph G {\n");
        sb.append("    rankdir=").append(orientation).append(";\n");
        sb.append("    node [shape=box, style=\"filled,rounded\", fillcolor=\"#f0f4f8\", color=\"#334155\", fontname=\"Arial\"];\n");
        sb.append("    edge [fontname=\"Arial\", fontsize=10];\n");

        for (GraphEdge edge : edges) {
            String src = escapeDot(edge.source());
            String dest = escapeDot(edge.destination());
            String flux = escapeDot(edge.flux());

            if (src.isEmpty() && dest.isEmpty()) continue;

            String edgeStyle = getDotEdgeStyle(edge.flux());

            if (flux.isEmpty()) {
                sb.append(String.format("    \"%s\" -> \"%s\"%s;\n", src, dest, edgeStyle));
            } else {
                sb.append(String.format("    \"%s\" -> \"%s\" [label=\"%s\"%s];\n", src, dest, flux, edgeStyle.isEmpty() ? "" : ", " + edgeStyle.substring(2, edgeStyle.length() - 1)));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String getDotEdgeStyle(String flux) {
        String lower = flux.toLowerCase(Locale.ROOT);
        if (lower.contains("kafka") || lower.contains("event") || lower.contains("queue") || lower.contains("mq")) {
            return " [color=\"#f57c00\", style=dashed]";
        } else if (lower.contains("rest") || lower.contains("http") || lower.contains("api")) {
            return " [color=\"#0288d1\", style=solid]";
        } else if (lower.contains("sql") || lower.contains("db") || lower.contains("query")) {
            return " [color=\"#388e3c\", style=bold]";
        }
        return "";
    }

    private String sanitize(String val) {
        return val.replace("\"", "'");
    }

    private String escapeDot(String val) {
        return val.replace("\"", "\\\"");
    }
}
