package com.raytheon.ooi.driver_control;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;

public class Main extends Application {
    private ControlWindow controlWindow;

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // create window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ControlWindow.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 600);
            controlWindow = loader.getController();
            stage.setTitle("DriverControl");
            stage.setScene(scene);
            stage.show();

            Parameters parameters = getParameters();
            if (parameters.getNamed().containsKey("config")) {
                File path = Paths.get(parameters.getNamed().get("config")).toFile();
                controlWindow.loadConfig(path);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        if (controlWindow.driverProcess != null) {
            controlWindow.shutdownDriver();
            controlWindow.driverProcess.destroy();
        }
//        if (controlWindow.driverInterface != null)
//            controlWindow.driverInterface.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
