package com.raytheon.ooi.driver_control;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class DriverModel {
    private DriverConfig config;
    private static Logger log = LogManager.getLogger("DriverModel");

    protected final ObservableList<ProtocolCommand> commandList = FXCollections.observableArrayList();
    protected final ObservableList<Parameter> paramList = FXCollections.observableArrayList();
    protected final ObservableList<String> sampleTypes = FXCollections.observableArrayList();

    protected Map<String, ObservableList<Map<String, Object>>> sampleLists = new HashMap<>();
    protected Map<String, ProtocolCommand> commands = new HashMap<>();
    protected Map<String, Parameter> parameters = new HashMap<>();

    private SimpleStringProperty state = new SimpleStringProperty();
    private SimpleStringProperty status = new SimpleStringProperty();
    private SimpleStringProperty connection = new SimpleStringProperty();

    private SimpleBooleanProperty paramsSettable = new SimpleBooleanProperty();

    public DriverModel() {
        log.debug("Created driver model");
    }

    public String maybeString(Object s) {
        if (s == null) {
            return "";
        }
        if (s instanceof String) {
            return (String) s;
        }
        return s.toString();
    }

    public String getString(JSONObject object, String key) {
        if (object.containsKey(key)) {
            Object val = object.get(key);
            return maybeString(val);
        }
        return "";
    }

    public void parseMetadata(JSONObject metadata) {
        log.debug("parseMetadata: {}", metadata);
        JSONObject _commands = (JSONObject) metadata.get("commands");
        JSONObject _parameters = (JSONObject) metadata.get("parameters");
        for (Object _name: _commands.keySet()) {
            String name = (String) _name;
            String displayName = getString((JSONObject)_commands.get(name), "display_name");
            ProtocolCommand command = new ProtocolCommand(name, displayName);
            commands.put(name, command);
        }

        for (Object _name: _parameters.keySet()) {
            String name = (String) _name;
            JSONObject param = (JSONObject) _parameters.get(name);
            String displayName = getString(param, "display_name");
            String visibility = getString(param, "visibility");
            String description = getString(param, "description");

            JSONObject value = (JSONObject) param.get("value");
            String valueDescription = getString(value, "description");
            String valueType = getString(value, "type");
            String units = getString(value, "units");
            Parameter paramObj = new Parameter(name, displayName, description, visibility,
                    valueDescription, valueType, units);
            parameters.put(name, paramObj);
            paramList.add(paramObj);
        }
        log.debug(commands);
        log.debug(parameters);
    }

    public void parseCapabilities(JSONArray capes) {
        log.debug("parse capabilities, clearing commandList");
        commandList.clear();
        setParamsSettable(false);
        for (Object cape : capes) {
            log.debug("CAPABILITY: {}", cape);
            String capability = (String) cape;
            log.debug("Found capability: " + capability);
            ProtocolCommand command = commands.get(capability);
            if (command == null) {
                command = new ProtocolCommand(capability, "");
                commands.put(capability, command);
            }
            if (command.getName().equals("DRIVER_EVENT_GET")) continue;
            if (command.getName().equals("DRIVER_EVENT_SET")) {
                setParamsSettable(true);
                continue;
            }
            log.debug("Adding capability: " + command);
            commandList.add(command);
        }
    }

    public String getState() {
        return state.get();
    }

    public void setState(String state) {
        if (state != null) {
            this.state.set(state);
        }
    }

    public SimpleStringProperty getStateProperty() {
        return state;
    }

    public void setParams(JSONObject params) {
        if (params != null) {
            for (Object key : params.keySet()) {
                String name = (String) key;
                String value = getString(params, name);
                if (name != null) {
                    Parameter param = parameters.get(name);
                    if (param != null) {
                        if (!Objects.equals(param.getValue(), value)) {
                            log.debug("UPDATED PARAM: " + name + " VALUE: " + value);
                            param.setValue(value);
                        }
                    }
                }
            }
        }
    }

    protected void publishSample(Map<String, Object> sample) {
        String streamName = (String) sample.get(DriverSampleFactory.STREAM_NAME);
        Platform.runLater(()->{
            if (!sampleLists.containsKey(streamName)) {
                sampleLists.put(streamName, FXCollections.observableArrayList(new ArrayList<Map<String, Object>>()));
                sampleTypes.add(streamName);
            }
            try {
                List<Map<String, Object>> samples = sampleLists.get(streamName);
                if (samples != null) samples.add(sample);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean getParamsSettable() {
        return paramsSettable.get();
    }

    public void setParamsSettable(boolean paramsSettable) {
        this.paramsSettable.set(paramsSettable);
    }

    public SimpleBooleanProperty getParamsSettableProperty() {
        return paramsSettable;
    }

    public void setConfig(DriverConfig config) {
        this.config = config;
    }

    public DriverConfig getConfig() {
        return config;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public SimpleStringProperty getStatusProperty() {
        return status;
    }

    public String getConnection() {
        return connection.get();
    }

    public void setConnection(String connection) {
        this.connection.set(connection);
    }

    public SimpleStringProperty getConnectionProperty() {
        return connection;
    }
}
