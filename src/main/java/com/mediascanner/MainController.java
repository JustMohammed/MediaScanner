package com.mediascanner;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

    @FXML private VBox rootVBox;
    @FXML private ComboBox<String> driveCombo;
    @FXML private Button browseBtn;
    @FXML private Button scanBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> extensionCombo;
    @FXML private ToggleButton filterAll;
    @FXML private ToggleButton filterImages;
    @FXML private ToggleButton filterVideos;
    @FXML private TableView<MediaFile> fileTable; 
    @FXML private TableColumn<MediaFile, String> nameColumn;
    @FXML private TableColumn<MediaFile, String> pathColumn;
    @FXML private TableColumn<MediaFile, String> typeColumn;

    // Window Management Variables
    private double xOffset = 0;
    private double yOffset = 0;
    private double lastX, lastY, lastWidth, lastHeight;
    private boolean isMaximized = false;

    private final ObservableList<MediaFile> allFiles = FXCollections.observableArrayList();
    private FilteredList<MediaFile> filteredFiles;
    private final FileScanner scanner = new FileScanner();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean scanning = false;
    private String selectedPath = null;

    @FXML
    public void initialize() {
        // --- Custom Window Drag Logic ---
        rootVBox.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        rootVBox.setOnMouseDragged(event -> {
            if (!isMaximized) {
                Stage stage = (Stage) rootVBox.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // Setup drives dropdown
        driveCombo.getItems().addAll(FileScanner.getAvailableDrives());
        if (!driveCombo.getItems().isEmpty()) {
            driveCombo.setValue(driveCombo.getItems().get(0));
            selectedPath = driveCombo.getValue();
        }

        // Setup extension dropdown
        extensionCombo.getItems().add("All Formats");
        extensionCombo.setValue("All Formats");
        extensionCombo.valueProperty().addListener((obs, old, val) -> applyFilter());

        // Setup table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        // UI Enhancements: Auto-resize columns and allow multi-selection
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        filteredFiles = new FilteredList<>(allFiles, p -> true);
        fileTable.setItems(filteredFiles);

        searchField.textProperty().addListener((obs, old, val) -> applyFilter());

        // Setup Filter Toggles
        ToggleGroup tg = new ToggleGroup();
        filterAll.setToggleGroup(tg);
        filterImages.setToggleGroup(tg);
        filterVideos.setToggleGroup(tg);
        filterAll.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == null) filterAll.setSelected(true);
            applyFilter();
        });

        // Double click to open folder
        fileTable.setRowFactory(tv -> {
            TableRow<MediaFile> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openFileLocation(row.getItem());
                }
            });
            return row;
        });

        cancelBtn.setDisable(true);
        progressBar.setVisible(false);

        driveCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) selectedPath = val;
        });
    }

    @FXML
    private void handleClose() {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleMinimize() {
        ((Stage) rootVBox.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) rootVBox.getScene().getWindow();
        if (!isMaximized) {
            // Store current bounds before maximizing
            lastX = stage.getX();
            lastY = stage.getY();
            lastWidth = stage.getWidth();
            lastHeight = stage.getHeight();

            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
            isMaximized = true;
        } else {
            // Restore previous bounds
            stage.setX(lastX);
            stage.setY(lastY);
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);
            isMaximized = false;
        }
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder to Scan");
        File dir = chooser.showDialog(browseBtn.getScene().getWindow());
        if (dir != null) {
            selectedPath = dir.getAbsolutePath();
            statusLabel.setText("Selected: " + selectedPath);
        }
    }

    @FXML
    private void onScan() {
        if (selectedPath == null) return;

        allFiles.clear();
        scanning = true;
        scanBtn.setDisable(true);
        cancelBtn.setDisable(false);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Scanning...");

        executor.submit(() -> scanner.scanDirectory(selectedPath, new FileScanner.ScanProgressListener() {
            @Override
            public void onProgress(String currentPath, int found) {
                if (!scanning) return;
                Platform.runLater(() -> {
                    statusLabel.setText("Found " + found + " files");
                    statsLabel.setText(currentPath);
                });
            }

            @Override
            public void onComplete(List<MediaFile> files) {
                Platform.runLater(() -> {
                    allFiles.addAll(files);
                    updateExtensionCombo(files);
                    statusLabel.setText("Scan complete! Total: " + files.size());
                    progressBar.setProgress(1.0);
                    scanBtn.setDisable(false);
                    cancelBtn.setDisable(true);
                    scanning = false;
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + message);
                    scanBtn.setDisable(false);
                    cancelBtn.setDisable(true);
                });
            }
        }));
    }

    @FXML
    private void handleDelete() {
        ObservableList<MediaFile> selectedItems = fileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            statusLabel.setText("Please select files to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Bulk Delete");
        alert.setHeaderText("Delete " + selectedItems.size() + " files?");
        alert.setContentText("This action cannot be undone.");

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("root-container");

        if (alert.showAndWait().get() == ButtonType.OK) {
            List<MediaFile> toDelete = new ArrayList<>(selectedItems);
            int deletedCount = 0;
            for (MediaFile file : toDelete) {
                try {
                    Files.delete(Paths.get(file.getPath()));
                    allFiles.remove(file);
                    deletedCount++;
                } catch (Exception e) {
                    System.err.println("Failed to delete: " + file.getPath());
                }
            }
            statusLabel.setText("Deleted " + deletedCount + " files.");
            applyFilter();
        }
    }

    private void updateExtensionCombo(List<MediaFile> files) {
        Set<String> exts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (MediaFile f : files) {
            exts.add(f.getExtension());
        }
        extensionCombo.getItems().clear();
        extensionCombo.getItems().add("All Formats");
        extensionCombo.getItems().addAll(exts);
        extensionCombo.setValue("All Formats");
    }

    @FXML
    private void onCancel() {
        scanning = false;
        statusLabel.setText("Cancelled.");
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase();
        String selectedExt = extensionCombo.getValue();

        filteredFiles.setPredicate(file -> {
            boolean typeMatch = true;
            if (filterImages.isSelected()) typeMatch = file.getMediaType() == MediaFile.MediaType.IMAGE;
            else if (filterVideos.isSelected()) typeMatch = file.getMediaType() == MediaFile.MediaType.VIDEO;

            boolean searchMatch = search.isEmpty() || file.getName().toLowerCase().contains(search);
            boolean extMatch = selectedExt == null || selectedExt.equals("All Formats") || file.getExtension().equalsIgnoreCase(selectedExt);

            return typeMatch && searchMatch && extMatch;
        });

        statsLabel.setText("Showing " + filteredFiles.size() + " of " + allFiles.size() + " files");
    }

    private void openFileLocation(MediaFile file) {
        try {
            Desktop.getDesktop().open(new File(file.getPath()).getParentFile());
        } catch (Exception e) {
            statusLabel.setText("Error opening location.");
        }
    }
}