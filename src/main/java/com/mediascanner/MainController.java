package com.mediascanner;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

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
    @FXML private TableView<MediaFile> tableView;
    @FXML private TableColumn<MediaFile, String> colName;
    @FXML private TableColumn<MediaFile, String> colType;
    @FXML private TableColumn<MediaFile, String> colExtension;
    @FXML private TableColumn<MediaFile, String> colSize;
    @FXML private TableColumn<MediaFile, String> colPath;

    private final ObservableList<MediaFile> allFiles = FXCollections.observableArrayList();
    private FilteredList<MediaFile> filteredFiles;
    private final FileScanner scanner = new FileScanner();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean scanning = false;
    private String selectedPath = null;

    @FXML
    public void initialize() {
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
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colExtension.setCellValueFactory(new PropertyValueFactory<>("extension"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colPath.setCellValueFactory(new PropertyValueFactory<>("path"));

        // Filtered list
        filteredFiles = new FilteredList<>(allFiles, p -> true);
        tableView.setItems(filteredFiles);

        // Search
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());

        // Filter toggles
        ToggleGroup tg = new ToggleGroup();
        filterAll.setToggleGroup(tg);
        filterImages.setToggleGroup(tg);
        filterVideos.setToggleGroup(tg);
        filterAll.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == null) filterAll.setSelected(true);
            applyFilter();
        });

        // Double click to open file location
        tableView.setRowFactory(tv -> {
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
    private void onBrowse() {
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
        if (selectedPath == null) {
            statusLabel.setText("Please select a drive or folder first.");
            return;
        }

        allFiles.clear();
        scanning = true;
        scanBtn.setDisable(true);
        cancelBtn.setDisable(false);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Scanning " + selectedPath + " ...");
        statsLabel.setText("");

        executor.submit(() -> scanner.scanDirectory(selectedPath, new FileScanner.ScanProgressListener() {
            @Override
            public void onProgress(String currentPath, int found) {
                if (!scanning) return;
                Platform.runLater(() -> {
                    statusLabel.setText("Scanning... Found " + found + " files");
                    String truncated = currentPath.length() > 80
                        ? "..." + currentPath.substring(currentPath.length() - 80)
                        : currentPath;
                    statsLabel.setText(truncated);
                });
            }

            @Override
            public void onComplete(List<MediaFile> files) {
                Platform.runLater(() -> {
                    allFiles.addAll(files);
                    
                    // Extract unique extensions for the dropdown
                    Set<String> uniqueExtensions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    for (MediaFile f : files) {
                        uniqueExtensions.add(f.getExtension());
                    }
                    
                    // Update extension dropdown
                    extensionCombo.getItems().clear();
                    extensionCombo.getItems().add("All Formats");
                    extensionCombo.getItems().addAll(uniqueExtensions);
                    extensionCombo.setValue("All Formats");

                    long images = files.stream().filter(f -> f.getMediaType() == MediaFile.MediaType.IMAGE).count();
                    long videos = files.stream().filter(f -> f.getMediaType() == MediaFile.MediaType.VIDEO).count();
                    statusLabel.setText("Scan complete!  Total: " + files.size() + " files");
                    statsLabel.setText("Images: " + images + "   Videos: " + videos);
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
                    progressBar.setVisible(false);
                    scanBtn.setDisable(false);
                    cancelBtn.setDisable(true);
                    scanning = false;
                });
            }
        }));
    }

    @FXML
    private void onCancel() {
        scanning = false;
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
        statusLabel.setText("Scan cancelled.");
        progressBar.setVisible(false);
        scanBtn.setDisable(false);
        cancelBtn.setDisable(true);
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase();
        String selectedExt = extensionCombo.getValue();

        filteredFiles.setPredicate(file -> {
            // Type Match (Images vs Videos)
            boolean typeMatch = true;
            if (filterImages.isSelected()) typeMatch = file.getMediaType() == MediaFile.MediaType.IMAGE;
            else if (filterVideos.isSelected()) typeMatch = file.getMediaType() == MediaFile.MediaType.VIDEO;

            // Search Match (Text field)
            boolean searchMatch = search.isEmpty()
                || file.getName().toLowerCase().contains(search)
                || file.getPath().toLowerCase().contains(search)
                || file.getExtension().toLowerCase().contains(search);

            // Extension Match (Dropdown)
            boolean extMatch = selectedExt == null 
                || selectedExt.equals("All Formats") 
                || file.getExtension().equalsIgnoreCase(selectedExt);

            return typeMatch && searchMatch && extMatch;
        });

        statsLabel.setText("Showing " + filteredFiles.size() + " of " + allFiles.size() + " files");
    }

    private void openFileLocation(MediaFile file) {
        try {
            File f = new File(file.getPath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f.getParentFile());
            }
        } catch (Exception e) {
            statusLabel.setText("Could not open folder: " + e.getMessage());
        }
    }
}