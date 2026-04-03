package com.mediascanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.InputStream;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // --- UPDATED FILENAME HERE ---
        try {
            // Ensure the file is in src/main/resources/mediamanagericon.png
            InputStream iconStream = getClass().getResourceAsStream("/mediamanagericon.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else {
                System.out.println("ERROR: mediamanagericon.png not found in src/main/resources/");
            }
        } catch (Exception e) {
            System.out.println("Error loading icon: " + e.getMessage());
        }
        // ------------------------------

        stage.setTitle("MediaScanner — Find Your Files");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}