package com.mediascanner;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainController {

    @FXML private VBox rootVBox;
    @FXML private HBox titleBar;
    @FXML private ComboBox<String> driveCombo;
    @FXML private Button browseBtn;
    @FXML private Button scanBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> extensionCombo;
    @FXML private ComboBox<String> durationCombo;
    @FXML private ToggleButton filterAll;
    @FXML private ToggleButton filterImages;
    @FXML private ToggleButton filterVideos;
    @FXML private TableView<MediaFile> fileTable; 
    @FXML private TableColumn<MediaFile, String> nameColumn;
    @FXML private TableColumn<MediaFile, String> pathColumn;
    @FXML private TableColumn<MediaFile, String> typeColumn;
    @FXML private TableColumn<MediaFile, String> durationColumn;
    @FXML private TableColumn<MediaFile, String> dateColumn;

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
        extensionCombo.valueProperty().addListener((obs, old, val) -> {
            applyFilter();
        });

        // Setup duration dropdown
        durationCombo.getItems().addAll("Any", "1 Min+", "3 Min+", "5 Min+");
        durationCombo.setValue("Any");
        durationCombo.valueProperty().addListener((obs, old, val) -> {
            applyFilter();
        });

        // Setup table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationDisplay"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("creationDate"));

        // UI Enhancements
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Sorting Logic
        filteredFiles = new FilteredList<>(allFiles, p -> true);
        SortedList<MediaFile> sortedFiles = new SortedList<>(filteredFiles);
        sortedFiles.comparatorProperty().bind(fileTable.comparatorProperty());
        fileTable.setItems(sortedFiles);

        searchField.textProperty().addListener((obs, old, val) -> {
            applyFilter();
        });

        // Setup Filter Toggles
        ToggleGroup tg = new ToggleGroup();
        filterAll.setToggleGroup(tg);
        filterImages.setToggleGroup(tg);
        filterVideos.setToggleGroup(tg);
        filterAll.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == null) {
                filterAll.setSelected(true);
            }
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
            if (val != null) {
                selectedPath = val;
            }
        });
    }

    @FXML
    private void handleZip() {
        ObservableList<MediaFile> selectedItems = fileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            statusLabel.setText("Please select files to zip.");
            return;
        }

        // Custom Dialog Setup
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Zip Archive");
        dialog.setHeaderText("Zipping " + selectedItems.size() + " files");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("root-container");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField zipNameField = new TextField("media_archive");
        zipNameField.setPromptText("Filename");
        TextField zipLocField = new TextField(selectedPath != null ? selectedPath : "");
        Button browseZipLoc = new Button("...");

        browseZipLoc.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(rootVBox.getScene().getWindow());
            if (f != null) zipLocField.setText(f.getAbsolutePath());
        });

        grid.add(new Label("Archive Name:"), 0, 0);
        grid.add(zipNameField, 1, 0);
        grid.add(new Label("Destination:"), 0, 1);
        grid.add(zipLocField, 1, 1);
        grid.add(browseZipLoc, 2, 1);

        dialogPane.setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().get() == ButtonType.OK) {
            String fullPath = zipLocField.getText() + File.separator + zipNameField.getText() + ".zip";
            
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        progressBar.setVisible(true);
                        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        statusLabel.setText("Creating zip...");
                    });

                    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fullPath))) {
                        for (MediaFile media : selectedItems) {
                            File fileToZip = new File(media.getPath());
                            zos.putNextEntry(new ZipEntry(fileToZip.getName()));
                            Files.copy(fileToZip.toPath(), zos);
                            zos.closeEntry();
                        }
                        Platform.runLater(() -> {
                            statusLabel.setText("Success: " + zipNameField.getText() + ".zip");
                            progressBar.setProgress(1.0);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> statusLabel.setText("Zip Error: " + e.getMessage()));
                    }
                }
            });
        }
    }

    @FXML private void handleClose() { Platform.exit(); System.exit(0); }
    @FXML private void handleMinimize() { ((Stage) rootVBox.getScene().getWindow()).setIconified(true); }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) rootVBox.getScene().getWindow();
        if (!isMaximized) {
            lastX = stage.getX(); lastY = stage.getY();
            lastWidth = stage.getWidth(); lastHeight = stage.getHeight();
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX()); stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth()); stage.setHeight(bounds.getHeight());
            isMaximized = true;
        } else {
            stage.setX(lastX); stage.setY(lastY);
            stage.setWidth(lastWidth); stage.setHeight(lastHeight);
            isMaximized = false;
        }
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
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

        executor.submit(new Runnable() {
            @Override
            public void run() {
                scanner.scanDirectory(selectedPath, new FileScanner.ScanProgressListener() {
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
                            statusLabel.setText("Total: " + files.size());
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
                        });
                    }
                });
            }
        });
    }

    @FXML
    private void handleDelete() {
        ObservableList<MediaFile> selectedItems = fileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete " + selectedItems.size() + " files?");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("root-container");

        if (alert.showAndWait().get() == ButtonType.OK) {
            List<MediaFile> toDelete = new ArrayList<>(selectedItems);
            for (MediaFile file : toDelete) {
                try {
                    Files.delete(Paths.get(file.getPath()));
                    allFiles.remove(file);
                } catch (Exception e) {}
            }
            applyFilter();
        }
    }

    private void updateExtensionCombo(List<MediaFile> files) {
        Set<String> exts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (MediaFile f : files) { exts.add(f.getExtension()); }
        extensionCombo.getItems().clear();
        extensionCombo.getItems().add("All Formats");
        extensionCombo.getItems().addAll(exts);
        extensionCombo.setValue("All Formats");
    }

    @FXML private void onCancel() { scanning = false; statusLabel.setText("Cancelled."); }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase();
        String selectedExt = extensionCombo.getValue();
        String selectedDur = durationCombo.getValue();

        filteredFiles.setPredicate(file -> {
            boolean typeMatch = true;
            if (filterImages.isSelected()) typeMatch = file.getMediaType() == MediaFile.MediaType.IMAGE;
            else if (filterVideos.isSelected()) typeMatch = file.getMediaType() == MediaFile.MediaType.VIDEO;

            if (typeMatch && filterVideos.isSelected() && selectedDur != null && !selectedDur.equals("Any")) {
                double mins = file.getEstimatedMinutes();
                if (selectedDur.equals("1 Min+") && mins < 1) return false;
                if (selectedDur.equals("3 Min+") && mins < 3) return false;
                if (selectedDur.equals("5 Min+") && mins < 5) return false;
            }

            boolean searchMatch = search.isEmpty() || file.getName().toLowerCase().contains(search);
            boolean extMatch = selectedExt == null || selectedExt.equals("All Formats") || file.getExtension().equalsIgnoreCase(selectedExt);
            return typeMatch && searchMatch && extMatch;
        });
        statsLabel.setText("Showing " + filteredFiles.size() + " of " + allFiles.size());
    }

    private void openFileLocation(MediaFile file) {
        try { Desktop.getDesktop().open(new File(file.getPath()).getParentFile()); }
        catch (Exception e) {}
    }
}