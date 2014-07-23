package com.raytheon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {
    private static Logger log = LogManager.getLogger();

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // create window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ControlWindow.fxml"));
            Parent root = (Parent) loader.load();
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("DriverControl");
            stage.setScene(scene);
            stage.show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}
