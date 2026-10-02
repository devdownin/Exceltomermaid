package com.example.excelgraph.model;

public record GraphEdge(String source, String flux, String destination, String component, boolean isExternal) {
    public GraphEdge(String source, String flux, String destination, String component) {
        this(source, flux, destination, component, false);
    }

    public GraphEdge(String source, String flux, String destination) {
        this(source, flux, destination, "", false);
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
