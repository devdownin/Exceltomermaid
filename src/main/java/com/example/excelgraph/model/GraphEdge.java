package com.example.excelgraph.model;

public record GraphEdge(String source, String flux, String destination, String component) {
    public GraphEdge(String source, String flux, String destination) {
        this(source, flux, destination, "");
    }

    public GraphEdge {
        if (source == null) source = "";
        if (flux == null) flux = "";
        if (destination == null) destination = "";
        if (component == null) component = "";
        source = source.trim();
        flux = flux.trim();
        destination = destination.trim();
        component = component.trim();
    }
}
