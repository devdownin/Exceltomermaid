package com.example.excelgraph.service;

import com.example.excelgraph.model.GraphEdge;
import com.example.excelgraph.model.GraphResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphGeneratorService {

    public GraphResponse generateGraphResponse(List<GraphEdge> edges) {
        Set<String> nodes = new LinkedHashSet<>();
        for (GraphEdge edge : edges) {
            if (!edge.source().isEmpty()) nodes.add(edge.source());
            if (!edge.destination().isEmpty()) nodes.add(edge.destination());
        }

        String mermaid = generateMermaidCode(edges);
        String dot = generateDotCode(edges);

        return new GraphResponse(edges, nodes, mermaid, dot);
    }

    private String generateMermaidCode(List<GraphEdge> edges) {
        StringBuilder sb = new StringBuilder("graph LR\n");
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
        return sb.toString();
    }

    private String generateDotCode(List<GraphEdge> edges) {
        StringBuilder sb = new StringBuilder("digraph G {\n");
        sb.append("    rankdir=LR;\n");
        sb.append("    node [shape=box, style=filled, fillcolor=lightgoldenrodyellow];\n");
        for (GraphEdge edge : edges) {
            String src = escapeDot(edge.source());
            String dest = escapeDot(edge.destination());
            String flux = escapeDot(edge.flux());

            if (src.isEmpty() && dest.isEmpty()) continue;

            if (flux.isEmpty()) {
                sb.append(String.format("    \"%s\" -> \"%s\";\n", src, dest));
            } else {
                sb.append(String.format("    \"%s\" -> \"%s\" [label=\"%s\"];\n", src, dest, flux));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String sanitize(String val) {
        return val.replace("\"", "'");
    }

    private String escapeDot(String val) {
        return val.replace("\"", "\\\"");
    }
}
