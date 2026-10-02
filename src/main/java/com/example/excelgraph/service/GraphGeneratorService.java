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

    private static final String[] PALETTE = {
            "#0288d1", "#f57c00", "#388e3c", "#7b1fa2", "#c2185b", "#00796b", "#d32f2f", "#5d4037", "#455a64"
    };

    private String generateMermaidCode(List<GraphEdge> edges, Set<String> nodes, String orientation) {
        StringBuilder sb = new StringBuilder("flowchart ").append(orientation).append("\n");

        Set<String> externalNodes = new HashSet<>();
        Set<String> standardDestNodes = new HashSet<>();
        for (GraphEdge edge : edges) {
            if (!edge.destination().isEmpty()) {
                if (edge.isExternal()) {
                    externalNodes.add(edge.destination());
                } else {
                    standardDestNodes.add(edge.destination());
                }
            }
        }

        Map<String, String> nodeToId = new LinkedHashMap<>();
        List<String> srcNodeIds = new ArrayList<>();
        List<String> destNodeIds = new ArrayList<>();
        List<String> extNodeIds = new ArrayList<>();

        int idCounter = 1;
        for (String node : nodes) {
            String nodeId = "N" + idCounter++;
            nodeToId.put(node, nodeId);
            sb.append(String.format("    %s[\"%s\"]\n", nodeId, sanitize(node)));

            if (externalNodes.contains(node)) {
                extNodeIds.add(nodeId);
            } else if (standardDestNodes.contains(node)) {
                destNodeIds.add(nodeId);
            } else {
                srcNodeIds.add(nodeId);
            }
        }

        sb.append("\n");
        sb.append("    classDef srcNode fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#0d47a1;\n");
        sb.append("    classDef destNode fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:#1b5e20;\n");
        sb.append("    classDef extNode fill:#fff3e0,stroke:#ef6c00,stroke-width:2px,color:#e65100;\n");

        if (!srcNodeIds.isEmpty()) {
            sb.append("    class ").append(String.join(",", srcNodeIds)).append(" srcNode;\n");
        }
        if (!destNodeIds.isEmpty()) {
            sb.append("    class ").append(String.join(",", destNodeIds)).append(" destNode;\n");
        }
        if (!extNodeIds.isEmpty()) {
            sb.append("    class ").append(String.join(",", extNodeIds)).append(" extNode;\n");
        }

        sb.append("\n");

        Map<String, String> componentColors = buildComponentColorMap(edges);
        List<String> edgeLinkColors = new ArrayList<>();

        for (GraphEdge edge : edges) {
            String srcId = nodeToId.get(edge.source());
            String destId = nodeToId.get(edge.destination());
            String flux = sanitize(edge.flux());
            String comp = sanitize(edge.component());

            if (srcId == null && destId == null) continue;

            String label = buildMermaidLabel(flux, comp);

            if (srcId != null && destId != null) {
                if (label.isEmpty()) {
                    sb.append(String.format("    %s --> %s\n", srcId, destId));
                } else {
                    sb.append(String.format("    %s -->|\"%s\"| %s\n", srcId, label, destId));
                }
                String color = componentColors.getOrDefault(edge.component().toLowerCase(Locale.ROOT).trim(), "#333333");
                edgeLinkColors.add(color);
            } else if (srcId != null) {
                sb.append(String.format("    %s\n", srcId));
            } else {
                sb.append(String.format("    %s\n", destId));
            }
        }

        if (!edgeLinkColors.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < edgeLinkColors.size(); i++) {
                sb.append(String.format("    linkStyle %d stroke:%s,stroke-width:2px;\n", i, edgeLinkColors.get(i)));
            }
        }

        return sb.toString();
    }

    private Map<String, String> buildComponentColorMap(List<GraphEdge> edges) {
        Map<String, String> map = new LinkedHashMap<>();
        int colorIdx = 0;
        for (GraphEdge edge : edges) {
            String compKey = edge.component().toLowerCase(Locale.ROOT).trim();
            if (!compKey.isEmpty() && !map.containsKey(compKey)) {
                map.put(compKey, PALETTE[colorIdx % PALETTE.length]);
                colorIdx++;
            }
        }
        return map;
    }

    private String buildMermaidLabel(String flux, String component) {
        if (flux.isEmpty() && component.isEmpty()) return "";
        if (component.isEmpty()) return flux;
        if (flux.isEmpty()) return "[" + component + "]";
        return flux + "<br/>[" + component + "]";
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
            String comp = escapeDot(edge.component());

            if (src.isEmpty() && dest.isEmpty()) continue;

            String label = buildDotLabel(flux, comp);
            String edgeStyle = getDotEdgeStyle(edge.flux(), edge.component());

            if (label.isEmpty()) {
                sb.append(String.format("    \"%s\" -> \"%s\"%s;\n", src, dest, edgeStyle));
            } else {
                sb.append(String.format("    \"%s\" -> \"%s\" [label=\"%s\"%s];\n", src, dest, label, edgeStyle.isEmpty() ? "" : ", " + edgeStyle.substring(2, edgeStyle.length() - 1)));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String buildDotLabel(String flux, String component) {
        if (flux.isEmpty() && component.isEmpty()) return "";
        if (component.isEmpty()) return flux;
        if (flux.isEmpty()) return "[" + component + "]";
        return flux + "\\n[" + component + "]";
    }

    private String getDotEdgeStyle(String flux, String component) {
        String lower = (flux + " " + component).toLowerCase(Locale.ROOT);
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
