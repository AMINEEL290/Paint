package com.paint;

import com.paint.model.DrawingTool;
import com.paint.service.FileService;
import com.paint.view.DrawingCanvas;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class Main extends Application {
    private final DrawingCanvas drawingCanvas = new DrawingCanvas();
    private final Map<DrawingTool, ToggleButton> toolButtons = new EnumMap<>(DrawingTool.class);
    private final Label statusLabel = new Label("Prêt");
    private final Label dimensionsLabel = new Label();
    private Stage stage;
    private File currentFile;
    private ColorPicker primaryPicker;
    private ColorPicker secondaryPicker;
    private TextField widthField;
    private TextField heightField;
    private StackPaneWrapper canvasWrapper;
    private double zoom = 1.0;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createTopBar());
        root.setLeft(createToolPanel());
        root.setRight(createSettingsPanel());
        root.setCenter(createWorkspace());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
        installShortcuts(scene);
        drawingCanvas.setColorPicked(color -> {
            primaryPicker.setValue(color);
            drawingCanvas.setPrimaryColor(color);
        });
        drawingCanvas.setStatusChanged(statusLabel::setText);
        updateDimensions();
        stage.setTitle("Paint Desktop");
        stage.setMinWidth(980);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createTopBar() {
        Button newButton = button("Nouveau", "Ctrl+N", event -> newDocument());
        Button openButton = button("Ouvrir", "Ctrl+O", event -> openDocument());
        Button saveButton = button("Enregistrer", "Ctrl+S", event -> saveDocument(false));
        Button saveAsButton = button("Enregistrer sous", "Ctrl+Maj+S", event -> saveDocument(true));
        Button undoButton = button("Annuler", "Ctrl+Z", event -> undo());
        Button redoButton = button("Rétablir", "Ctrl+Y", event -> redo());
        Button exportButton = button("Exporter", "PNG / JPEG", event -> saveDocument(true));
        Button zoomOut = button("−", "Réduire le zoom", event -> changeZoom(-0.1));
        Button zoomIn = button("+", "Augmenter le zoom", event -> changeZoom(0.1));
        HBox actions = new HBox(8, newButton, openButton, saveButton, saveAsButton,
                new Separator(), undoButton, redoButton, new Separator(), exportButton,
                new Separator(), zoomOut, zoomIn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 16, 10, 16));
        actions.getStyleClass().add("top-toolbar");

        Label title = new Label("PAINT DESKTOP");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Atelier de dessin");
        subtitle.getStyleClass().add("app-subtitle");
        VBox branding = new VBox(1, title, subtitle);
        HBox header = new HBox(12, branding, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 18, 8, 18));
        HBox.setHgrow(actions, Priority.ALWAYS);
        return new VBox(header);
    }

    private VBox createToolPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(16, 12, 16, 12));
        panel.setPrefWidth(172);
        panel.getStyleClass().add("side-panel");
        Label heading = new Label("OUTILS");
        heading.getStyleClass().add("section-title");
        GridPane grid = new GridPane();
        grid.setHgap(7);
        grid.setVgap(7);
        ToggleGroup group = new ToggleGroup();
        int index = 0;
        for (DrawingTool tool : DrawingTool.values()) {
            ToggleButton button = new ToggleButton(tool.toString());
            button.setToggleGroup(group);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefWidth(72);
            button.setPrefHeight(44);
            button.setTooltip(new Tooltip(tool.toString()));
            button.setOnAction(event -> drawingCanvas.setTool(tool));
            toolButtons.put(tool, button);
            grid.add(button, index % 2, index / 2);
            index++;
        }
        toolButtons.get(DrawingTool.PENCIL).setSelected(true);
        panel.getChildren().addAll(heading, grid);
        return panel;
    }

    private VBox createSettingsPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(228);
        panel.getStyleClass().add("side-panel");

        Label colorsHeading = new Label("COULEURS");
        colorsHeading.getStyleClass().add("section-title");
        primaryPicker = new ColorPicker(Color.BLACK);
        secondaryPicker = new ColorPicker(Color.WHITE);
        primaryPicker.setMaxWidth(Double.MAX_VALUE);
        secondaryPicker.setMaxWidth(Double.MAX_VALUE);
        primaryPicker.setOnAction(event -> drawingCanvas.setPrimaryColor(primaryPicker.getValue()));
        secondaryPicker.setOnAction(event -> drawingCanvas.setSecondaryColor(secondaryPicker.getValue()));
        Button swapColors = new Button("Échanger principal / secondaire");
        swapColors.setMaxWidth(Double.MAX_VALUE);
        swapColors.setOnAction(event -> {
            Color primary = primaryPicker.getValue();
            primaryPicker.setValue(secondaryPicker.getValue());
            secondaryPicker.setValue(primary);
        });
        Label primaryLabel = new Label("Couleur principale");
        Label secondaryLabel = new Label("Couleur secondaire");
        VBox colors = new VBox(5, primaryLabel, primaryPicker, secondaryLabel, secondaryPicker, swapColors);
        GridPane palette = createColorPalette();

        Label brushHeading = new Label("ÉPAISSEUR");
        brushHeading.getStyleClass().add("section-title");
        Slider widthSlider = new Slider(1, 48, 3);
        widthSlider.setShowTickLabels(true);
        widthSlider.setMajorTickUnit(12);
        widthSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                drawingCanvas.setStrokeWidth(newValue.doubleValue()));
        Label widthLabel = new Label("3 px");
        widthSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                widthLabel.setText(String.format("%.0f px", newValue.doubleValue())));
        VBox brushSettings = new VBox(5, widthLabel, widthSlider);

        Label documentHeading = new Label("DOCUMENT");
        documentHeading.getStyleClass().add("section-title");
        widthField = new TextField("1200");
        heightField = new TextField("760");
        widthField.setPrefColumnCount(5);
        heightField.setPrefColumnCount(5);
        GridPane dimensions = new GridPane();
        dimensions.setHgap(8);
        dimensions.setVgap(6);
        dimensions.addRow(0, new Label("Largeur"), widthField);
        dimensions.addRow(1, new Label("Hauteur"), heightField);
        Button resizeButton = new Button("Redimensionner");
        resizeButton.setMaxWidth(Double.MAX_VALUE);
        resizeButton.setOnAction(event -> resizeDocument());

        panel.getChildren().addAll(colorsHeading, colors, palette, new Separator(), brushHeading,
                brushSettings, new Separator(), documentHeading, dimensions, resizeButton);
        return panel;
    }

    private ScrollPane createWorkspace() {
        canvasWrapper = new StackPaneWrapper(drawingCanvas);
        ScrollPane scrollPane = new ScrollPane(canvasWrapper);
        scrollPane.setPannable(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(false);
        scrollPane.getStyleClass().add("workspace");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(16, statusLabel, dimensionsLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                this::undo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN),
                this::redo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                this::newDocument);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                this::openDocument);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
                () -> saveDocument(false));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S,
                        KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> saveDocument(true));
    }

    private Button button(String text, String tooltip, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(action);
        button.getStyleClass().add("toolbar-button");
        return button;
    }

    private void newDocument() {
        TextField width = new TextField("1200");
        TextField height = new TextField("760");
        GridPane fields = new GridPane();
        fields.setHgap(8);
        fields.setVgap(8);
        fields.addRow(0, new Label("Largeur (64–4096 px)"), width);
        fields.addRow(1, new Label("Hauteur (64–4096 px)"), height);
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Nouveau document");
        dialog.setHeaderText("Dimensions de la nouvelle toile");
        dialog.getDialogPane().setContent(fields);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> buttonType == javafx.scene.control.ButtonType.OK);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(ignored -> {
            try {
                drawingCanvas.newDocument(Integer.parseInt(width.getText()), Integer.parseInt(height.getText()));
                currentFile = null;
                updateDimensions();
                stage.setTitle("Paint Desktop — Sans titre");
            } catch (IllegalArgumentException exception) {
                showError(exception.getMessage());
            }
        });
    }

    private void openDocument() {
        FileChooser chooser = imageChooser("Ouvrir une image");
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Image image = FileService.open(file);
            drawingCanvas.openImage(image);
            currentFile = file;
            updateDimensions();
            stage.setTitle("Paint Desktop — " + file.getName());
        } catch (IOException | IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void saveDocument(boolean saveAs) {
        File file = currentFile;
        if (saveAs || file == null) {
            FileChooser chooser = imageChooser("Enregistrer le dessin");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            file = chooser.showSaveDialog(stage);
        }
        if (file == null) {
            return;
        }
        String format = formatFor(file);
        if (!"png".equals(format) && !"jpg".equals(format) && !"jpeg".equals(format)) {
            showError("Choisissez une extension .png, .jpg ou .jpeg.");
            return;
        }
        try {
            FileService.save(drawingCanvas.snapshot(), file, format);
            currentFile = file;
            stage.setTitle("Paint Desktop — " + file.getName());
            statusLabel.setText("Enregistré : " + file.getName());
        } catch (IOException exception) {
            showError(exception.getMessage());
        }
    }

    private FileChooser imageChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images PNG", "*.png"),
                new FileChooser.ExtensionFilter("Images JPEG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Images BMP", "*.bmp"),
                new FileChooser.ExtensionFilter("Toutes les images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        return chooser;
    }

    private static String formatFor(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "png" : name.substring(dot + 1).toLowerCase();
    }

    private void resizeDocument() {
        try {
            drawingCanvas.resizeDocument(Integer.parseInt(widthField.getText()),
                    Integer.parseInt(heightField.getText()));
            currentFile = null;
            updateDimensions();
            statusLabel.setText("Nouveau document redimensionné");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void updateDimensions() {
        dimensionsLabel.setText(drawingCanvas.documentWidth() + " × "
                + drawingCanvas.documentHeight() + " px");
        if (canvasWrapper != null) {
            canvasWrapper.setZoom(zoom);
        }
        if (widthField != null) {
            widthField.setText(Integer.toString(drawingCanvas.documentWidth()));
            heightField.setText(Integer.toString(drawingCanvas.documentHeight()));
        }
    }

    private GridPane createColorPalette() {
        Color[] colors = {
                Color.BLACK, Color.WHITE, Color.DARKSLATEGRAY, Color.GRAY, Color.SILVER,
                Color.RED, Color.ORANGERED, Color.ORANGE, Color.GOLD, Color.YELLOW,
                Color.GREEN, Color.LIMEGREEN, Color.TEAL, Color.CYAN, Color.DODGERBLUE,
                Color.BLUE, Color.INDIGO, Color.PURPLE, Color.MAGENTA, Color.HOTPINK
        };
        GridPane palette = new GridPane();
        palette.setHgap(5);
        palette.setVgap(5);
        for (int index = 0; index < colors.length; index++) {
            Color color = colors[index];
            Button swatch = new Button();
            swatch.setPrefSize(28, 24);
            swatch.setStyle("-fx-background-color: " + color.toString().replace("0x", "#")
                    + "; -fx-border-color: #d3dae3; -fx-border-radius: 3; -fx-background-radius: 3;");
            swatch.setTooltip(new Tooltip(color.toString()));
            swatch.setOnAction(event -> primaryPicker.setValue(color));
            palette.add(swatch, index % 5, index / 5);
        }
        return palette;
    }

    private void undo() {
        drawingCanvas.undo();
        updateDimensions();
    }

    private void redo() {
        drawingCanvas.redo();
        updateDimensions();
    }

    private void changeZoom(double increment) {
        zoom = Math.max(0.25, Math.min(2.0, zoom + increment));
        canvasWrapper.setZoom(zoom);
        statusLabel.setText("Zoom : " + Math.round(zoom * 100) + "%");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Paint Desktop");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message == null ? "Une erreur inattendue est survenue." : message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final class StackPaneWrapper extends javafx.scene.layout.StackPane {
        private final DrawingCanvas canvas;
        private final Scale canvasScale = new Scale(1, 1, 0, 0);

        private StackPaneWrapper(DrawingCanvas canvas) {
            this.canvas = canvas;
            getChildren().add(canvas);
            setPadding(new Insets(28));
            setAlignment(Pos.TOP_LEFT);
            canvas.getTransforms().add(canvasScale);
            setZoom(1);
        }

        private void setZoom(double zoom) {
            canvasScale.setX(zoom);
            canvasScale.setY(zoom);
            double contentWidth = canvas.getWidth() * zoom + 56;
            double contentHeight = canvas.getHeight() * zoom + 56;
            setMinSize(contentWidth, contentHeight);
            setPrefSize(contentWidth, contentHeight);
            setMaxSize(contentWidth, contentHeight);
        }
    }
}
