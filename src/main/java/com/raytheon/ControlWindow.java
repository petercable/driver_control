package com.raytheon;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ControlWindow {
    @FXML
    AnchorPane root;
    @FXML
    private TableView<ProtocolCommand> commandTable;
    @FXML
    private TableView<Parameter> parameterTable;
    @FXML
    private TableColumn<ProtocolCommand, String> commandColumn;
    @FXML
    private TableColumn<ProtocolCommand, String> commandNameColumn;
    @FXML
    private TableColumn<Parameter, String> parameterNameColumn;
    @FXML
    private TableColumn<Parameter, String> parameterValueColumn;
    @FXML
    private TableColumn<Parameter, String> parameterNewValueColumn;
    @FXML
    public TextArea console;
    @FXML
    private TextField stateField;
    @FXML
    private TextField statusField;
    @FXML
    private Button sendParamButton;
    @FXML
    private TabPane tabPane;


    private DriverModel model = new DriverModel();
    private DriverControl controller;
    private EventListener listener;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    // Listeners
    private ChangeListener<String> stateListener = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
            stateField.setText((observableValue).getValue());
        }
    };

    private ChangeListener<Boolean> settableListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean s, Boolean s2) {
            parameterNewValueColumn.setEditable(observableValue.getValue());
            sendParamButton.setVisible(observableValue.getValue());
        }
    };

    private ListChangeListener<String> sampleChangeListener = new ListChangeListener<String>() {
        @Override
        public void onChanged(final Change<? extends String> change) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    while (change.next()) {
                        for (String sample : change.getAddedSubList()) {
                            log.debug("added sample type: " + sample);
                            // new sample type detected
                            // create a new tab
                            Tab tab = new Tab(sample);
                            tabPane.getTabs().add(tab);

                            // create a tableview, add it to the tab
                            TableView<Map<String, Object>> tableView = new TableView<Map<String, Object>>(model.sampleLists.get(sample));
                            tab.setContent(tableView);

                            // grab a sample, use it to find the columns and populate
                            // the tableview...
                            Map<String, Object> oneSample = model.sampleLists.get(sample).get(0);
                            List<String> keys = new ArrayList<String>(oneSample.keySet());
                            Collections.sort(keys);
                            for (String key: keys) {
                                TableColumn<Map<String, Object>, String> column = new TableColumn<Map<String, Object>, String>(key);
                                column.setCellValueFactory(new MapValueFactory(key));
                                column.setPrefWidth(50.0);
                                tableView.getColumns().add(column);
                            }
                        }
                    }
                }
            });
        }
    };
    private String driverPath;

    @FXML
    private void initialize() {
        commandColumn.setCellValueFactory(new PropertyValueFactory<ProtocolCommand, String>("name"));
        commandNameColumn.setCellValueFactory(new PropertyValueFactory<ProtocolCommand, String>("displayName"));

        parameterNameColumn.setCellValueFactory(new PropertyValueFactory<Parameter, String>("displayName"));
        parameterValueColumn.setCellValueFactory(new PropertyValueFactory<Parameter, String>("value"));
        parameterNewValueColumn.setCellValueFactory(new PropertyValueFactory<Parameter, String>("newValue"));
        parameterNewValueColumn.setCellFactory(TextFieldTableCell.<Parameter>forTableColumn());

        parameterNewValueColumn.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent<Parameter, String>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<Parameter, String> t) {
                        log.debug("setOnEditCommit");
                        t.getTableView().getItems().get(t.getTablePosition().getRow()).setNewValue(t.getNewValue());
                    }
                }
        );
    }

    private int getPort(String filename) throws Exception {
        Path path = Paths.get(filename);
        String contents = new String(Files.readAllBytes(path));
        return Integer.parseInt(contents.trim());
    }

    public void selectCommand(MouseEvent event) {
        try {
            log.debug("received event: " + event);
            TableView source = (TableView) event.getSource();
            int row = source.getSelectionModel().getSelectedIndex();
            ProtocolCommand command = model.commandList.get(row);
            log.debug("maybe I clicked: " + command);
            controller.execute(command.getName());
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public void sendParams() {
        log.debug("clicked send params");
        Map<String, Object> values = new HashMap<String, Object>();
        for (Parameter p: model.parameters.values()) {
            Object sendValue;
            String newValue = p.getNewValue();
            String oldValue = p.getValue();
            if (newValue.equals("")) continue;
            if (newValue.equals(oldValue)) continue;
            String type = p.getValueType();
            if (type.equals("int")) {
                sendValue = Integer.parseInt(newValue);
            } else if (type.equals("float")) {
                sendValue = Double.parseDouble(newValue);
            } else {
                sendValue = newValue;
            }
            values.put(p.getName(), sendValue);
        }
        for (Parameter p: model.parameters.values()) {
            p.setNewValue("");
        }
        controller.setResource(new JSONObject(values).toString());
    }

    public void getConfig() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Driver Config");
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) {
            boolean success = false;
            DriverConfig config = null;
            String error = "";
            try {
                config = new DriverConfig(file);
                success = true;
            } catch (IOException e) {
                error = "(Unable to open the specified file)";
            } catch (URISyntaxException e) {
                error = "(Unable to parse egg URL)";
            } catch (ClassCastException e) {
                error = "(Unable to parse YAML)";
            }
            if (success) {
                model.setConfig(config);
                console.appendText(config.toString());
                statusField.setText("config file parsed successfully!");
            } else {
                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.setScene(new Scene(VBoxBuilder.create().
                        children(new Label("Unable to load configuration file!"), new Label(error)).
                        alignment(Pos.CENTER).padding(new Insets(15)).build()));
                dialogStage.show();
            }
        }
    }

    public void launchDriver() throws IOException, InterruptedException {
        /// build command to exec:
        /// cd ~/Workspace/eggs/thsph/
        /// ./launch_driver --event_port_file=/tmp/event_port --command_port_file=/tmp/command_port
        if (this.driverPath == null)
        {
            //user prompt "Enter path to driver launch script"
            this.driverPath = "~/Workspace/eggs/thsph";
        }
        Process p = Runtime.getRuntime().exec("cd " + this.driverPath);

        Runtime.getRuntime().exec("./launch_driver --event_port_file=/tmp/event_port --command_port_file=/tmp/command_port");
    }

    public void zmqConnect() {
        // create model and controllers
        DriverConfig config = model.getConfig();
        if (config != null) {
            try {
                String host = config.getHost();
                int eventPort = getPort(config.getEventPortFile());
                int commandPort = getPort(config.getCommandPortFile());
                controller = new DriverControl(host, commandPort, model);
                listener = new EventListener(host, eventPort, model, controller);
                listener.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void configure() {
        controller.getProtocolState();
        String state = model.getState();
        log.debug("Called configure.  Current state: " + state + " Configuration: " + model.getConfig());
        if (Objects.equals(state, "DRIVER_STATE_UNCONFIGURED")) {
            controller.configure();
            controller.init();
        }
    }

    public void connect() {
        controller.getProtocolState();
        String state = model.getState();
        log.debug("Called connect.  Current state: " + state);
        if (Objects.equals(state, "DRIVER_STATE_DISCONNECTED")) {
            controller.connect();
        }
    }

    public void discover() {
        controller.getProtocolState();
        String state = model.getState();
        log.debug("Called discover.  Current state: " + state);
        if (Objects.equals(state, "DRIVER_STATE_UNKNOWN")) {
            controller.discover();
        }
    }

    public void validateStreams() {
        log.debug("Pete is too lazy to implement this.");
        /// get preload URL from user
        /// make sure we have a data stream capture from driver
        /// prompt user to select data stream name from list
        /// compare data stream fields with those listed in preload
    }

    public void displayTestProcedures() {
        log.debug("Dan hasn't figured out how to create another window in Scene Builder, but Pete will in two seconds...");
    }
}
