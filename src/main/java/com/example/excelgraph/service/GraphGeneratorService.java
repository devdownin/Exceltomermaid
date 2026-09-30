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

        String mermaid = generateMermaidCode(edges, nodes, orientation);
        String dot = generateDotCode(edges, orientation);

        return new GraphResponse(edges, nodes, mermaid, dot);
    }

    private String generateMermaidCode(List<GraphEdge> edges, Set<String> nodes, String orientation) {
        StringBuilder sb = new StringBuilder("flowchart ").append(orientation).append("\n");

        // Map each node name to a safe Mermaid identifier (e.g. N1, N2...)
        Map<String, String> nodeToId = new LinkedHashMap<>();
        int idCounter = 1;
        for (String node : nodes) {
            String nodeId = "N" + idCounter++;
            nodeToId.put(node, nodeId);
            sb.append(String.format("    %s[\"%s\"]\n", nodeId, sanitize(node)));
        }

        sb.append("\n");

        for (GraphEdge edge : edges) {
            String srcId = nodeToId.get(edge.source());
            String destId = nodeToId.get(edge.destination());
            String flux = sanitize(edge.flux());

            if (srcId == null && destId == null) continue;

            if (srcId != null && destId != null) {
                if (flux.isEmpty()) {
                    sb.append(String.format("    %s --> %s\n", srcId, destId));
                } else {
                    sb.append(String.format("    %s -->|\"%s\"| %s\n", srcId, flux, destId));
                }
            } else if (srcId != null) {
                sb.append(String.format("    %s\n", srcId));
            } else {
                sb.append(String.format("    %s\n", destId));
            }
        }

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
