package com.raytheon;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
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
    @FXML
    private TextArea driverLogArea;
    @FXML
    private Button refreshLogButton;

    private DriverModel model = new DriverModel();
    private DriverControl controller;
    protected EventListener listener;
    private PreloadDatabase preload;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    // Listeners
    private ChangeListener<String> stateListener = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
            Platform.runLater(() -> stateField.setText((observableValue).getValue()));
        }
    };

    private ChangeListener<Boolean> settableListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean s, Boolean s2) {
            Platform.runLater(() -> {
                    parameterNewValueColumn.setEditable(observableValue.getValue());
                    sendParamButton.setVisible(observableValue.getValue());
                });
        }
    };

    private ChangeListener<String> statusListener = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
            Platform.runLater(() -> statusField.setText((observableValue).getValue()));
        }
    };

    private ListChangeListener<String> sampleChangeListener = new ListChangeListener<String>() {
        @Override
        public void onChanged(final Change<? extends String> change) {
            Platform.runLater(() -> {
                while (change.next()) {
                    for (String sample : change.getAddedSubList()) {
                        log.debug("added sample type: " + sample);
                        // new sample type detected
                        // create a new tab
                        Tab tab = new Tab(sample);
                        tabPane.getTabs().add(tab);

                        // create a tableview, add it to the tab
                        TableView<Map<String, Object>> tableView = new TableView<>(model.sampleLists.get(sample));
                        tab.setContent(tableView);

                        // grab a sample, use it to find the columns and populate
                        // the tableview...
                        Map<String, Object> oneSample = model.sampleLists.get(sample).get(0);
                        List<String> keys = new ArrayList<>(oneSample.keySet());
                        Collections.sort(keys);
                        for (String key: keys) {
                            TableColumn<Map<String, Object>, String> column = new TableColumn<>(key);
                            column.setCellValueFactory(new MapValueFactory(key));
                            column.setPrefWidth(50.0);
                            tableView.getColumns().add(column);
                        }
                    }
                }
            });
        }
    };

    @FXML
    private void initialize() {
        commandColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        commandNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));

        parameterNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        parameterValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        parameterNewValueColumn.setCellValueFactory(new PropertyValueFactory<>("newValue"));
        parameterNewValueColumn.setCellFactory(TextFieldTableCell.<Parameter>forTableColumn());

        parameterNewValueColumn.setOnEditCommit(
                t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setNewValue(t.getNewValue())
        );

        commandTable.setItems(model.commandList);
        parameterTable.setItems(model.paramList);
        this.model.getStateProperty().addListener(stateListener);
        this.model.getParamsSettableProperty().addListener(settableListener);
        this.model.getStatusProperty().addListener(statusListener);
        this.model.sampleTypes.addListener(sampleChangeListener);
        refreshLogButton.setVisible(false);
    }

    private int getPort(String filename) throws Exception {
        Path path = Paths.get(filename);
        String contents = new String(Files.readAllBytes(path));
        return Integer.parseInt(contents.trim());
    }

    public void selectCommand(MouseEvent event) {
        if (! checkController()) return;
            log.debug("received event: " + event);
            TableView source = (TableView) event.getSource();
            int row = source.getSelectionModel().getSelectedIndex();
            ProtocolCommand command = model.commandList.get(row);
            log.debug("maybe I clicked: " + command);
            controller.execute(command.getName());
    }

    public void sendParams() {
        if (! checkController()) return;
        log.debug("clicked send params");
        Map<String, Object> values = new HashMap<>();
        for (Parameter p: model.parameters.values()) {
            Object sendValue;
            String newValue = p.getNewValue();
            String oldValue = p.getValue();
            if (newValue.equals("")) continue;
            if (newValue.equals(oldValue)) continue;
            String type = p.getValueType();
            switch (type) {
                case "int":
                    sendValue = Integer.parseInt(newValue);
                    break;
                case "float":
                    sendValue = Double.parseDouble(newValue);
                    break;
                default:
                    sendValue = newValue;
                    break;
            }
            values.put(p.getName(), sendValue);
        }
        for (Parameter p: model.parameters.values()) {
            p.setNewValue("");
        }
        controller.setResource(new JSONObject(values).toString());
    }

    public void refreshDriverLog() {
        File file = new File("/tmp/mi-drivers.log");

        try (FileReader in=new FileReader(file)) {
            char[] buffer = new char[4096];
            int len;
            driverLogArea.setText("");
            while ((len = in.read(buffer)) != -1) {
                String s = new String(buffer, 0, len);
                driverLogArea.appendText(s);
            }
            // driverLogArea.setCaretPosition(0);
        }
        catch (IOException e) {
            driverLogArea.setText(e.getClass().getName() + ": " + e.getMessage());
            Dialogs.create()
                    .owner(null)
                    .title("Driver Log")
                    .message("Unable to load driver log.")
                    .showException(e);
        }
    }

    public boolean loadConfig() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Driver Config");
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) {
            DriverConfig config;

            try {
                config = new DriverConfig(file);
            } catch (IOException e) {
                Dialogs.create()
                        .owner(null)
                        .title("Load Configuration Exception")
                        .message("Unable to parse configuration. Configuration must be valid yaml file.")
                        .showException(e);
                return false;
            }

            model.setConfig(config);
            console.appendText(config.toString());
            try {
                preload = new PreloadDatabase(SqliteConnectionFactory.getConnection(config.getDatabaseFile()));
            } catch (SQLException | ClassNotFoundException e) {
                Dialogs.create()
                        .owner(null)
                        .title("Preload Database")
                        .message("Exception connecting to preload DB.")
                        .showException(e);

            }
            statusField.setText("config file parsed successfully!");
        }
        return true;
    }

    public DriverConfig getConfig() {
        DriverConfig config = model.getConfig();
        if (config == null) {
            Action response = Dialogs.create()
                    .owner(null)
                    .title("Test Configuration")
                    .message("Configuration has not been loaded. Load now?")
                    .actions(Dialog.Actions.YES, Dialog.Actions.NO)
                    .showConfirm();
            if (response == Dialog.Actions.YES) {
                this.loadConfig();
            }
        }
        return model.getConfig();
    }
    private void missingEnvironmentVariable(String envVar) {
        Dialogs.create()
                .owner(null)
                .title("Environment")
                .message("Missing required environment variable " + envVar)
                .showError();
    }

    public void launchDriver() throws IOException, InterruptedException {
        URL launch_url = getClass().getResource("/launch.py");
        DriverConfig config = this.getConfig();
        if (config == null) return;

        String egg_url = config.getEggUrl();

        String launch_file = launch_url.getFile();
        String path = "PATH=$PATH:" + System.getenv("PATH");
        String virtualEnv = System.getenv("VIRTUAL_ENV");
        if (virtualEnv == null) {
            missingEnvironmentVariable("VIRTUAL_ENV");
            return;
        }
        String[] args = {path};
        String python = virtualEnv + "/bin/python";
        String working_path = System.getenv("TEST_BASE");
        if (working_path == null) {
            missingEnvironmentVariable("TEST_BASE");
            return;
        }
        String[] command = {python, launch_file, working_path, egg_url};

        Runtime.getRuntime().exec(command, args);

        driverLogArea.setText("Driver started. Click Refresh for latest log data.");
        refreshLogButton.setVisible(true);
        // TODO - create a thread to monitor changes to log file and update window
    }

    public void zmqConnect() {
        // create model and controllers
        DriverConfig config = this.getConfig();
        model.setStatus("Connecting to driver...");
        try {
            String host = config.getHost();
            int eventPort = getPort(config.getEventPortFile());
            int commandPort = getPort(config.getCommandPortFile());
            controller = new DriverControl(host, commandPort, model);
            listener = new EventListener(host, eventPort, model, controller);
            listener.start();
            controller.getProtocolState();
            controller.getMetadata();
            model.setStatus("Connecting to driver...complete");
        } catch (Exception e) {
            Action response = Dialogs.create()
                    .owner(null)
                    .title("Driver Protocol Connection Exception")
                    .masthead("Exception occurred when attempting to connect to the protocol driver.")
                    .message("Unable to connect to driver. Would you like to launch the driver now?")
                    .actions(Dialog.Actions.YES, Dialog.Actions.NO)
                    .showConfirm();
            if (response == Dialog.Actions.YES) {
                try {
                    this.launchDriver();
                } catch (IOException | InterruptedException e1) {
                    Dialogs.create()
                            .owner(null)
                            .title("Launch Driver")
                            .message("Exception occurred launching driver.")
                            .showException(e);
                }
            }
            model.setStatus("Connecting to driver...failed");
        }
    }

    private boolean checkController() {
        if (controller == null) {
            Action response = Dialogs.create()
                    .owner(null)
                    .title("")
                    .message("Driver not yet connected. Connect now?")
                    .actions(Dialog.Actions.YES, Dialog.Actions.NO)
                    .showConfirm();
            if (response == Dialog.Actions.YES) {
                this.zmqConnect();
            }
        }
        return (controller != null);
    }

    public void configure() {
        if (! checkController()) return;
        controller.getProtocolState();
        String state = model.getState();
        if (Objects.equals(state, "DRIVER_STATE_UNCONFIGURED")) {
            model.setStatus("Configuring driver...");
            controller.configure();
            controller.init();
            model.setStatus("Configuration complete.");
        }
        else {
            model.setStatus("Configuration already complete.");
        }
    }

    public void connect() {
        if (! checkController()) return;
        controller.getProtocolState();
        String state = model.getState();
        if (Objects.equals(state, "DRIVER_STATE_DISCONNECTED")) {
            model.setStatus("Connecting to instrument...");
            controller.connect();
            model.setStatus("Connecting to instrument...done");
        }
    }

    public void getCapabilities() {
        if (! checkController()) return;
        controller.getCapabilities();  // immediate action
        model.setStatus("");
    }

    public void getParams() {
        if (! checkController()) return;
        model.setStatus("Getting parameters...");
        controller.getResource("DRIVER_PARAMETER_ALL");
    }

    public void discover() {
        if (! checkController()) return;
        controller.getProtocolState();
        String state = model.getState();
        if (Objects.equals(state, "DRIVER_STATE_UNKNOWN")) {
            model.setStatus("Discovering protocol state...");
            controller.discover();
            model.setStatus("Discovering protocol state...done");
        }
    }

    public void shutdownDriver() {
        if (! checkController()) return;
        controller.stop();
    }

    public void validateStreams() {
        preload.getParameter("PD190");
    }

    public void displayTestProcedures() {
        // needs controller?  load instructions?
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/HelpWindow.fxml"));
        Parent root = null;
        try {
            root = (Parent) loader.load();
            Scene scene = new Scene(root, 800, 600);
            Stage stage = new Stage();
            stage.setTitle("Help");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void exit() {
        ((Stage)root.getScene().getWindow()).close();
    }
}
