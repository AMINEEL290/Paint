package com.paint.view;

import com.paint.model.DrawingTool;
import com.paint.service.FloodFillService;
import com.paint.service.HistoryService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.function.Consumer;

public final class DrawingCanvas extends StackPane {
    public static final int MIN_SIZE = 64;
    public static final int MAX_SIZE = 4096;

    private final Canvas artwork = new Canvas(1200, 760);
    private final Canvas preview = new Canvas(1200, 760);
    private final GraphicsContext graphics = artwork.getGraphicsContext2D();
    private final GraphicsContext previewGraphics = preview.getGraphicsContext2D();
    private final HistoryService<WritableImage> history = new HistoryService<>(20);
    private DrawingTool tool = DrawingTool.PENCIL;
    private Color primaryColor = Color.BLACK;
    private Color secondaryColor = Color.WHITE;
    private double strokeWidth = 3;
    private double startX;
    private double startY;
    private double lastX;
    private double lastY;
    private boolean drawing;
    private Color activeColor = Color.BLACK;
    private Consumer<Color> colorPicked = color -> { };
    private Consumer<String> statusChanged = text -> { };

    public DrawingCanvas() {
        getStyleClass().add("canvas-frame");
        getChildren().addAll(artwork, preview);
        preview.setMouseTransparent(true);
        setMinSize(0, 0);
        setMaxSize(MAX_SIZE, MAX_SIZE);
        artwork.setOnMousePressed(this::beginAction);
        artwork.setOnMouseDragged(this::continueAction);
        artwork.setOnMouseReleased(this::finishAction);
        artwork.setOnMouseMoved(event -> statusChanged.accept(
                String.format("%.0f × %.0f px", event.getX(), event.getY())));
        clearToWhite();
        history.reset(snapshot());
    }

    public Canvas artwork() {
        return artwork;
    }

    public int documentWidth() {
        return (int) artwork.getWidth();
    }

    public int documentHeight() {
        return (int) artwork.getHeight();
    }

    public void setTool(DrawingTool tool) {
        this.tool = tool;
        artwork.setCursor(tool == DrawingTool.COLOR_PICKER
                ? javafx.scene.Cursor.CROSSHAIR : javafx.scene.Cursor.DEFAULT);
    }

    public void setPrimaryColor(Color color) {
        primaryColor = color;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public void setSecondaryColor(Color color) {
        secondaryColor = color;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public void setColorPicked(Consumer<Color> colorPicked) {
        this.colorPicked = colorPicked;
    }

    public void setStatusChanged(Consumer<String> statusChanged) {
        this.statusChanged = statusChanged;
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    public void undo() {
        history.undo().ifPresent(this::restore);
    }

    public void redo() {
        history.redo().ifPresent(this::restore);
    }

    public WritableImage snapshot() {
        return artwork.snapshot(null, new WritableImage(documentWidth(), documentHeight()));
    }

    public void openImage(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        validateDimensions(width, height);
        artwork.setWidth(width);
        artwork.setHeight(height);
        preview.setWidth(width);
        preview.setHeight(height);
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(image, 0, 0);
        history.setLimit(historyCapacity(width, height));
        history.reset(snapshot());
        requestLayout();
    }

    public void newDocument(int width, int height) {
        validateDimensions(width, height);
        artwork.setWidth(width);
        artwork.setHeight(height);
        preview.setWidth(width);
        preview.setHeight(height);
        clearToWhite();
        history.setLimit(historyCapacity(width, height));
        history.reset(snapshot());
        requestLayout();
    }

    public void resizeDocument(int width, int height) {
        validateDimensions(width, height);
        WritableImage previous = snapshot();
        artwork.setWidth(width);
        artwork.setHeight(height);
        preview.setWidth(width);
        preview.setHeight(height);
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(previous, 0, 0);
        history.setLimit(historyCapacity(width, height));
        history.record(snapshot());
        requestLayout();
    }

    private static void validateDimensions(int width, int height) {
        if (width < MIN_SIZE || height < MIN_SIZE || width > MAX_SIZE || height > MAX_SIZE) {
            throw new IllegalArgumentException("Les dimensions doivent être comprises entre "
                    + MIN_SIZE + " et " + MAX_SIZE + " pixels.");
        }
    }

    private static int historyCapacity(int width, int height) {
        long pixels = (long) width * height;
        return (int) Math.max(2, Math.min(20, (128L * 1024 * 1024) / (pixels * 4)));
    }

    private void clearToWhite() {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, artwork.getWidth(), artwork.getHeight());
    }

    private void beginAction(MouseEvent event) {
        startX = clampX(event.getX());
        startY = clampY(event.getY());
        lastX = startX;
        lastY = startY;
        if (tool == DrawingTool.COLOR_PICKER) {
            pickColor(startX, startY);
            return;
        }
        if (tool == DrawingTool.FILL) {
            floodFill((int) startX, (int) startY,
                    event.getButton() == MouseButton.SECONDARY ? secondaryColor : primaryColor);
            return;
        }
        if (tool == DrawingTool.TEXT) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Ajouter du texte");
            dialog.setHeaderText("Texte à ajouter au dessin");
            dialog.showAndWait().filter(text -> !text.isBlank()).ifPresent(text -> {
                graphics.setFill(event.getButton() == MouseButton.SECONDARY ? secondaryColor : primaryColor);
                graphics.setFont(Font.font(Math.max(12, strokeWidth * 6)));
                graphics.fillText(text, startX, startY);
                history.record(snapshot());
            });
            return;
        }
        drawing = true;
        activeColor = event.getButton() == MouseButton.SECONDARY ? secondaryColor : primaryColor;
        prepareStroke();
        if (tool == DrawingTool.PENCIL || tool == DrawingTool.BRUSH || tool == DrawingTool.ERASER) {
            graphics.beginPath();
            graphics.moveTo(startX, startY);
            graphics.lineTo(startX + 0.1, startY + 0.1);
            graphics.stroke();
        } else {
            drawPreview(startX, startY);
        }
    }

    private void continueAction(MouseEvent event) {
        if (!drawing) {
            return;
        }
        double x = clampX(event.getX());
        double y = clampY(event.getY());
        if (tool == DrawingTool.PENCIL || tool == DrawingTool.BRUSH || tool == DrawingTool.ERASER) {
            graphics.beginPath();
            graphics.moveTo(lastX, lastY);
            graphics.lineTo(x, y);
            graphics.stroke();
            lastX = x;
            lastY = y;
        } else {
            drawPreview(x, y);
        }
    }

    private void finishAction(MouseEvent event) {
        if (!drawing) {
            return;
        }
        double x = clampX(event.getX());
        double y = clampY(event.getY());
        if (tool != DrawingTool.PENCIL && tool != DrawingTool.BRUSH && tool != DrawingTool.ERASER) {
            previewGraphics.clearRect(0, 0, preview.getWidth(), preview.getHeight());
            drawShape(graphics, x, y);
        }
        drawing = false;
        history.record(snapshot());
    }

    private void prepareStroke() {
        graphics.setStroke(tool == DrawingTool.ERASER ? Color.WHITE : activeColor);
        double width = tool == DrawingTool.ERASER ? Math.max(strokeWidth * 4, 12)
                : tool == DrawingTool.PENCIL ? Math.min(strokeWidth, 2) : strokeWidth;
        graphics.setLineWidth(width);
        graphics.setLineCap(StrokeLineCap.ROUND);
        graphics.setLineJoin(StrokeLineJoin.ROUND);
    }

    private void drawPreview(double x, double y) {
        previewGraphics.clearRect(0, 0, preview.getWidth(), preview.getHeight());
        prepareStroke();
        drawShape(previewGraphics, x, y);
    }

    private void drawShape(GraphicsContext context, double x, double y) {
        double left = Math.min(startX, x);
        double top = Math.min(startY, y);
        double width = Math.abs(x - startX);
        double height = Math.abs(y - startY);
        context.setLineWidth(strokeWidth);
        context.setLineCap(StrokeLineCap.ROUND);
        switch (tool) {
            case LINE -> context.strokeLine(startX, startY, x, y);
            case RECTANGLE -> context.strokeRect(left, top, width, height);
            case FILLED_RECTANGLE -> {
                context.setFill(activeColor);
                context.fillRect(left, top, width, height);
            }
            case ELLIPSE -> context.strokeOval(left, top, width, height);
            case TRIANGLE -> {
                double[] xs = {startX, x, (startX + x) / 2};
                double[] ys = {y, y, startY};
                context.strokePolygon(xs, ys, 3);
            }
            default -> { }
        }
    }

    private void pickColor(double x, double y) {
        PixelReader reader = artwork.snapshot(null, null).getPixelReader();
        colorPicked.accept(reader.getColor((int) x, (int) y));
    }

    private void floodFill(int x, int y, Color replacement) {
        WritableImage before = snapshot();
        int width = documentWidth();
        int height = documentHeight();
        if (!FloodFillService.fill(before, x, y, replacement)) {
            return;
        }
        graphics.clearRect(0, 0, width, height);
        graphics.drawImage(before, 0, 0);
        history.record(snapshot());
    }

    private double clampX(double x) {
        return Math.max(0, Math.min(documentWidth() - 1, x));
    }

    private double clampY(double y) {
        return Math.max(0, Math.min(documentHeight() - 1, y));
    }

    private void restore(WritableImage image) {
        artwork.setWidth(image.getWidth());
        artwork.setHeight(image.getHeight());
        preview.setWidth(image.getWidth());
        preview.setHeight(image.getHeight());
        graphics.clearRect(0, 0, artwork.getWidth(), artwork.getHeight());
        graphics.drawImage(image, 0, 0);
        history.setLimit(historyCapacity(documentWidth(), documentHeight()));
        requestLayout();
    }
}
