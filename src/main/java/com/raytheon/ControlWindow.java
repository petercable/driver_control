package com.raytheon;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class ControlWindow {
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


    private DriverModel model;
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

    public void setup(DriverModel model, DriverControl controller, EventListener listener) {
        this.model = model;
        this.controller = controller;
        this.listener = listener;
        commandTable.setItems(model.commandList);
        parameterTable.setItems(model.paramList);
        this.model.getStateProperty().addListener(stateListener);
        this.model.getParamsSettableProperty().addListener(settableListener);
        this.model.sampleTypes.addListener(sampleChangeListener);
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
        log.debug("Dan hasn't figured out the yaml config file yet. Check back in two weeks.");
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
