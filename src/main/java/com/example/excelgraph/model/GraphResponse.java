package com.example.excelgraph.model;

import java.util.List;
import java.util.Set;

public record GraphResponse(
    List<GraphEdge> edges,
    Set<String> nodes,
    String mermaidCode,
    String dotCode
) {}
