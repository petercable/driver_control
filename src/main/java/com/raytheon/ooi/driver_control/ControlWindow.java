package com.raytheon.ooi.driver_control;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ControlWindow {
    @FXML AnchorPane root;
    @FXML private TableView<ProtocolCommand> commandTable;
    @FXML private TableView<Parameter> parameterTable;
    @FXML private TableColumn<ProtocolCommand, String> commandColumn;
    @FXML private TableColumn<ProtocolCommand, String> commandNameColumn;
    @FXML private TableColumn<Parameter, String> parameterNameColumn;
    @FXML private TableColumn<Parameter, String> parameterValueColumn;
    @FXML private TableColumn<Parameter, String> parameterNewValueColumn;
    @FXML public TextArea console;
    @FXML private TextField stateField;
    @FXML private TextField statusField;
    @FXML private Button sendParamButton;
    @FXML private TabPane tabPane;
    @FXML private TextArea driverLogArea;
    @FXML private Button refreshLogButton;

    private TabPane sampleTabPane;
    private DriverModel model = new DriverModel();
    protected DriverControl controller;
    protected EventListener listener;
    private PreloadDatabase preload;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();
    protected Process driverProcess = null;

    private ChangeListener<Boolean> settableListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean s, Boolean s2) {
            Platform.runLater(() -> {
                    parameterNewValueColumn.setEditable(observableValue.getValue());
                    sendParamButton.setVisible(observableValue.getValue());
                });
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
                        // create nested TabPane if necessary
                        if (null == sampleTabPane) {
                            Tab rootSampleDataTab = new Tab("Sample Data");
                            tabPane.getTabs().add(rootSampleDataTab);
                            sampleTabPane = new TabPane();
                            rootSampleDataTab.setContent(sampleTabPane);
                        }

                        // create a new sample/stream tab
                        Tab tab = new Tab(sample);
                        sampleTabPane.getTabs().add(tab);

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

        stateField.textProperty().bind(model.getStateProperty());
        statusField.textProperty().bind(model.getStatusProperty());

        this.model.getParamsSettableProperty().addListener(settableListener);
        this.model.sampleTypes.addListener(sampleChangeListener);
    }

    private int getPort(String filename) throws Exception {
        Path path = Paths.get(filename);
        String contents = new String(Files.readAllBytes(path));
        return Integer.parseInt(contents.trim());
    }

    public void selectCommand(MouseEvent event) {
        if (! checkController()) return;
            TableView source = (TableView) event.getSource();
            int row = source.getSelectionModel().getSelectedIndex();
            if (row != -1) {
                ProtocolCommand command = model.commandList.get(row);
                log.debug("Clicked: " + command);
                controller.execute(command.getName());
            }
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

    public void loadConfig() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Driver Config");
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        loadConfig(file);
    }

    public void loadConfig(File file) {
        log.debug("loading configuration from file: {}", file);
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
                return;
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
            model.setStatus("config file parsed successfully!");
        }
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


    public void launchDriver() throws IOException, InterruptedException {
        if (driverProcess != null) {
            shutdownDriver();
            driverProcess.destroy();
        }

        driverProcess = DriverLauncher.launchDriver(model.getConfig());
        watchStream(driverProcess.getErrorStream());
        watchStream(driverProcess.getInputStream());
    }

    public void watchStream(InputStream is) {
        Thread t = new Thread(() -> {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            while (true) {
                StringJoiner joiner = new StringJoiner("\n");
                try {
                    while (r.ready()) {
                        joiner.add(r.readLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (joiner.length() > 0)
                    Platform.runLater(() -> driverLogArea.appendText(joiner.toString()));

                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
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
            controller.getProtocolState().get();
            controller.getMetadata().get();
            model.setStatus("Connecting to driver...complete");
        } catch (Exception e) {
            e.printStackTrace();
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
        try {
            controller.getProtocolState().get();
            String state = model.getState();
            if (Objects.equals(state, "DRIVER_STATE_UNCONFIGURED")) {
                model.setStatus("Configuring driver...");
                controller.configure().get();
                controller.init().get();
                model.setStatus("Configuration complete.");
            }
            else {
                model.setStatus("Configuration already complete.");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        if (! checkController()) return;
        try {
            controller.getProtocolState().get();
            String state = model.getState();
            if (Objects.equals(state, "DRIVER_STATE_DISCONNECTED")) {
                model.setStatus("Connecting to instrument...");
                controller.connect().get();
                model.setStatus("Connecting to instrument...done");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void getCapabilities() {
        if (! checkController()) return;
        model.setStatus("Getting capabilities...");
        controller.getCapabilities();  // immediate action
    }

    public void getState() {
        if (! checkController()) return;
        model.setStatus("Getting protocol state...");
        controller.getProtocolState();  // immediate action
    }

    public void getParams() {
        if (! checkController()) return;
        model.setStatus("Getting parameters...");
        controller.getResource("DRIVER_PARAMETER_ALL");
    }

    public void discover() {
        if (! checkController()) return;
        try {
            controller.getProtocolState().get();
            String state = model.getState();
            if (Objects.equals(state, "DRIVER_STATE_UNKNOWN")) {
                model.setStatus("Discovering protocol state...");
                controller.discover();
                model.setStatus("Discovering protocol state...done");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void shutdownDriver() {
        if (! checkController()) return;
        controller.stop();
    }

    public void validateStreams() {

        Map<String, DataStream> map = preload.getStreams(model.getConfig().getScenario());
        map.entrySet().forEach(System.out::println);
        map.keySet().stream().filter(model.sampleLists::containsKey).forEach(key -> {
            ObservableList<Map<String, Object>> samples = model.sampleLists.get(key);
            Map sample = samples.get(0);
            DataStream ds = map.get(key);
            log.debug("going to compare {} to {}", sample, ds);
            for (String paramName : ds.getParams().keySet()) {
                DataParameter parameter = ds.getParams().get(paramName);
                if (!sample.containsKey(paramName))
                    log.error("MISSING PARAMETER FROM STREAM: {}", paramName);
                else {
                    Object value = sample.get(paramName);
                    log.debug("Testing {} value: {}", paramName, value);
                    switch (parameter.getValueEncoding()) {
                        case "int8":
                            if (!(value instanceof Integer)) {
                                log.error("Non integral value found in Integer field");
                                break;
                            }
                            if ((Integer) value > Byte.MAX_VALUE)
                                log.error("Oversized integral value found in Integer field");
                        case "int16":
                            if ((Integer) value > Short.MAX_VALUE)
                                log.error("Oversized integral value found in Integer field");
                        case "int32":
                            if ((Integer) value > Integer.MAX_VALUE)
                                log.error("Oversized integral value found in Integer field");
                            break;
                        case "float32":
                            if (!(value instanceof Double)) {
                                log.error("Non floating point value found in FP field");
                            }
                            if ((Float) value > Float.MAX_VALUE)
                                log.error("Oversized FP value found in FP field");
                        case "float64":
                            if ((Double) value > Double.MAX_VALUE)
                                log.error("Oversized FP value found in FP field");
                            break;
                        case "str":
                            break;
                        default:
                            log.error("UNHANDLED VALUE ENCODING {} {}", paramName, parameter.getValueEncoding());
                            break;
                    }
                }
            }
        });
    }

    public void displayTestProcedures() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/HelpWindow.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
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
