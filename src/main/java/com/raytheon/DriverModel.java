package com.raytheon;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class DriverModel {
    protected final ObservableList<ProtocolCommand> commandList = FXCollections.observableArrayList();
    protected final ObservableList<Parameter> paramList = FXCollections.observableArrayList();
    protected final ObservableList<String> sampleTypes = FXCollections.observableArrayList();
    protected Map<String, ObservableList<Map<String, Object>>> sampleLists = new HashMap<>();
    private static Logger log = LogManager.getLogger();
    private Map<String, ProtocolCommand> commands = new HashMap<>();
    protected Map<String, Parameter> parameters = new HashMap<>();
    private SimpleStringProperty state = new SimpleStringProperty();
    private SimpleBooleanProperty paramsSettable = new SimpleBooleanProperty();
    private DriverConfig config;
    private SimpleStringProperty status = new SimpleStringProperty();

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
        if (object.has(key)) {
            Object val = object.get(key);
            return maybeString(val);
        }
        return "";
    }

    public void parseMetadata(JSONObject metadata) {
        JSONObject _commands = metadata.getJSONObject("commands");
        JSONObject _parameters = metadata.getJSONObject("parameters");
        for (Object _name: _commands.keySet()) {
            String name = (String) _name;
            String displayName = getString(_commands.getJSONObject(name), "display_name");
            ProtocolCommand command = new ProtocolCommand(name, displayName);
            commands.put(name, command);
        }

        for (Object _name: _parameters.keySet()) {
            String name = (String) _name;
            JSONObject param = _parameters.getJSONObject(name);
            String displayName = getString(param, "display_name");
            String visibility = getString(param, "visibility");
            String description = getString(param, "description");

            JSONObject value = param.getJSONObject("value");
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
        for (int i=0; i<capes.length(); i++) {
            String capability = capes.getString(i);
            log.debug("Found capability: " + capability);
            ProtocolCommand command = commands.get(capability);
            if (command==null) {
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
                    if (!Objects.equals(param.getValue(), value)) {
                        log.debug("UPDATED PARAM: " + name + " VALUE: " + value);
                        param.setValue(value);
                    }
                }
            }
        }
    }

    protected void publishSample(Map<String, Object> sample) {
        String streamName = (String) sample.get(DriverSampleFactory.STREAM_NAME);

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
}
