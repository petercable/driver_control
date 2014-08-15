package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.driver_interface.DriverInterface;
import com.raytheon.ooi.driver_interface.ZmqDriverInterface;
import com.raytheon.ooi.preload.PreloadDatabase;
import com.raytheon.ooi.preload.SqlitePreloadDatabase;
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
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ControlWindow {
    @FXML AnchorPane root;
    @FXML private TableView<ProtocolCommand> commandTable;
    @FXML private TableView<Parameter> parameterTable;
    @FXML private TableColumn<ProtocolCommand, String> commandColumn;
    @FXML private TableColumn<ProtocolCommand, String> commandNameColumn;
    @FXML private TableColumn<Parameter, String> parameterNameColumn;
    @FXML private TableColumn<Parameter, String> parameterValueColumn;
    @FXML private TableColumn<Parameter, String> parameterNewValueColumn;
    @FXML private TextField stateField;
    @FXML private TextField statusField;
    @FXML private TextField connectionStatusField;
    @FXML private Button sendParamButton;
    @FXML private TabPane tabPane;

    private TabPane sampleTabPane;
    private static Logger log = LogManager.getLogger(ControlWindow.class);
    protected Process driverProcess = null;
    protected DriverInterface driverInterface = null;

    private final DriverModel model = DriverModel.getInstance();
    private final PreloadDatabase preload = SqlitePreloadDatabase.getInstance();
    private final DriverEventHandler eventHandler = new DriverEventHandler();

    private ChangeListener<Boolean> settableListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean s, Boolean s2) {
            Platform.runLater(() -> {
                    parameterNewValueColumn.setEditable(observableValue.getValue());
                    sendParamButton.setVisible(observableValue.getValue());
                });
        }
    };

    private ChangeListener<String> connectionChangeListener = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            Platform.runLater(()->connectionStatusField.setText(newValue));
        }
    };

    @SuppressWarnings("unchecked")
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
                        if (sample.equals("raw")) continue;
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
                            column.setPrefWidth(key.length() * 10);
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
        this.model.getConnectionProperty().addListener(connectionChangeListener);
    }

    public void selectCommand(MouseEvent event) {
        if (! checkController()) return;
            TableView source = (TableView) event.getSource();
            int row = source.getSelectionModel().getSelectedIndex();
            if (row != -1) {
                ProtocolCommand command = model.commandList.get(row);
                log.debug("Clicked: {}, {}", command, command.getName());
                model.commandList.clear();
                new Thread(()-> {
                    driverInterface.execute(command.getName());
                    Platform.runLater(this::getCapabilities);
                }).start();
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
        driverInterface.setResource(new JSONObject(values).toString());
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
            try {
                preload.connect(config);
                eventHandler.setConfig(config);
            } catch (Exception e) {
                Dialogs.create()
                        .owner(null)
                        .title("Preload Database")
                        .message("Exception connecting to preload DB.")
                        .showException(e);

            }
            model.setStatus("config file parsed successfully!");
        }
    }

    public void loadCoefficients() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Coefficient Config");
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        loadCoefficients(file);
    }

    public void loadCoefficients(File file) {
        log.debug("loading coefficients from file: {}", file);
        if (file != null) {
            try {
                model.setCoefficients(file);
            } catch (IOException e) {
                Dialogs.create()
                        .owner(null)
                        .title("Coefficient parse error")
                        .message("Coefficient parse error, file must be valid csv...")
                        .showException(e);
            }
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
    }

    public void driverConnect() {
        // create model and controllers
        DriverConfig config = this.getConfig();
        model.setStatus("Connecting to driver...");
        try {
            driverInterface = new ZmqDriverInterface(config.getHost(), config.getCommandPort(), config.getEventPort());
            driverInterface.addObserver(eventHandler);
            model.setStatus("Connecting to driver...complete");
        } catch (Exception e) {
            e.printStackTrace();
            Dialogs.create()
                    .owner(null)
                    .title("Driver Protocol Connection Exception")
                    .message("Exception occurred when attempting to connect to the protocol driver.")
                    .showException(e);
            model.setStatus("Connecting to driver...failed");
            return;
        }
        getMetadata();
        updateProtocolState();
        switch (model.getState()) {
            case Constants.DRIVER_STATE_UNCONFIGURED:
                configure();
            case Constants.DRIVER_STATE_DISCONNECTED:
                connect();
            case Constants.DRIVER_STATE_UNKNOWN:
                discover();
            default:
                getParams();
                updateProtocolState();
                break;
        }
    }

    private boolean checkController() {
        if (driverInterface == null) {
            Action response = Dialogs.create()
                    .owner(null)
                    .title("")
                    .message("Driver not yet connected. Connect now?")
                    .actions(Dialog.Actions.YES, Dialog.Actions.NO)
                    .showConfirm();
            if (response == Dialog.Actions.YES) {
                this.driverConnect();
            }
        }
        return (driverInterface != null && driverInterface.isConnected());
    }

    private void updateProtocolState() {
        model.setState(driverInterface.getProtocolState());
        log.debug("Protocol state in model set to: {}", model.getStatus());
    }

    public void configure() {
    if (! checkController()) return;
        updateProtocolState();
        String status;
        if (Objects.equals(model.getState(), Constants.DRIVER_STATE_UNCONFIGURED)) {
            model.setStatus("Configuring driver...");
            driverInterface.configurePortAgent(model.getConfig().getPortAgentConfig());
            driverInterface.initParams(model.getConfig().getStartupConfig());
            status = "Configuration complete.";
            updateProtocolState();
        }
        else {
            status = "Configuration already complete.";
        }
        model.setStatus(status);
        log.debug(status);
    }

    public void connect() {
        if (! checkController()) return;
        updateProtocolState();
        if (Objects.equals(model.getState(), Constants.DRIVER_STATE_DISCONNECTED)) {
            model.setStatus("Connecting to instrument...");
            driverInterface.connect();
            model.setStatus("Connecting to instrument...done");
            updateProtocolState();
        }
    }

    public void getCapabilities() {
        if (! checkController()) return;
        model.setStatus("Getting capabilities...");
        JSONArray capabilities = driverInterface.getCapabilities();
        model.parseCapabilities((JSONArray) capabilities.get(0));
    }

    public void getState() {
        if (! checkController()) return;
        model.setStatus("Getting protocol state...");
        updateProtocolState();
    }

    public void getParams() {
        if (! checkController()) return;
        model.setStatus("Getting parameters...");
        model.setParams((JSONObject) driverInterface.getResource("DRIVER_PARAMETER_ALL"));
    }

    public void getMetadata() {
        if (! checkController()) return;
        model.setStatus("Getting metadata...");
        JSONObject metadata = driverInterface.getMetadata();
        model.parseMetadata(metadata);
    }

    public void discover() {
        updateProtocolState();
        if (Objects.equals(model.getState(), Constants.DRIVER_STATE_UNKNOWN)) {
            model.setStatus("Discovering protocol state...");
            driverInterface.discoverState();
            model.setStatus("Discovering protocol state...done");
            updateProtocolState();
            getCapabilities();
        }
    }

    public void shutdownDriver() {
        if (! checkController()) return;
        driverInterface.stopDriver();
        driverInterface.shutdown();
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
