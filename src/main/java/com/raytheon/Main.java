package com.raytheon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Main extends Application {
    private static Logger log = LogManager.getLogger();
    private EventListener listener;

    private int getPort(String filename) throws Exception {
        Path path = Paths.get(filename);
        String contents = new String(Files.readAllBytes(path));
        return Integer.parseInt(contents.trim());
    }

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

            // get config
            Map params = getParameters().getNamed();
            int event_port = getPort((String) params.get("event_file"));
            int command_port = getPort((String) params.get("command_file"));
            String driver_host = "localhost";

            // create model and controllers
            DriverModel model = new DriverModel();
            DriverControl controller = new DriverControl(driver_host, command_port, model);
            listener = new EventListener(driver_host, event_port, model, controller);

            // register the model and controllers with the view
            ControlWindow controlWindow = loader.getController();
            controlWindow.setup(model, controller, listener);

            // initialize the driver
            listener.start();
            controller.ping();
            controller.getProtocolState();
            log.info(model.getState());
            if (model.getState().equals("DRIVER_STATE_UNCONFIGURED")) {
                controller.configure();
                controller.init();
                controller.connect();
                controller.discover();
            }
            controller.getMetadata();
            controller.getCapabilities();
            controller.getResource("DRIVER_PARAMETER_ALL");

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void stop() {
        log.info("Shutting down listener");
        listener.shutdown();
        log.info("Shutting down driver");
        //controller.stop();
        try {
            listener.join();
        } catch (InterruptedException e) {
            System.exit(1);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
