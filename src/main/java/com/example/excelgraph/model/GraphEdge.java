package com.example.excelgraph.model;

public record GraphEdge(String source, String flux, String destination) {
    public GraphEdge {
        if (source == null) source = "";
        if (flux == null) flux = "";
        if (destination == null) destination = "";
        source = source.trim();
        flux = flux.trim();
        destination = destination.trim();
    }
}
