package com.paint.model;

public enum DrawingTool {
    PENCIL("Crayon"),
    BRUSH("Pinceau"),
    ERASER("Gomme"),
    LINE("Ligne"),
    RECTANGLE("Rectangle"),
    FILLED_RECTANGLE("Rectangle plein"),
    ELLIPSE("Ellipse"),
    TRIANGLE("Triangle"),
    TEXT("Texte"),
    FILL("Pot de peinture"),
    COLOR_PICKER("Pipette");

    private final String label;

    DrawingTool(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
