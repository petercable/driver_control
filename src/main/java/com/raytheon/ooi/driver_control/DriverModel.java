package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.preload.DataStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class DriverModel {
    // static members, including singleton instance
    private final static DriverModel INSTANCE = new DriverModel();
    private static Logger log = LogManager.getLogger(DriverModel.class);

    // config is null until loaded
    private DriverConfig config;

    // coefficient data stored here
    private final Map<String, Object> coefficients = new HashMap<>();

    // storage for commandMetadata, parameterMetadata, samples
    protected final ObservableList<ProtocolCommand> commandList = FXCollections.observableArrayList();
    protected final ObservableList<Parameter> paramList = FXCollections.observableArrayList();
    protected final ObservableList<String> sampleTypes = FXCollections.observableArrayList();
    protected Map<String, ObservableList<DataStream>> sampleLists = new HashMap<>();
    protected Map<String, ProtocolCommand> commandMetadata = new HashMap<>();
    protected Map<String, Parameter> parameterMetadata = new HashMap<>();

    // properties to hold protocol state, connection and interface status
    private SimpleStringProperty state = new SimpleStringProperty();
    private SimpleStringProperty status = new SimpleStringProperty();
    private SimpleStringProperty connection = new SimpleStringProperty();

    // are params settable in current state
    private SimpleBooleanProperty paramsSettable = new SimpleBooleanProperty();

    public static DriverModel getInstance() {
        return INSTANCE;
    }

    private DriverModel() {

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

    public String getString(Map object, String key) {
        if (object.containsKey(key)) {
            Object val = object.get(key);
            return maybeString(val);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public void parseMetadata(Map metadata) {
        log.debug("parseMetadata: {}", metadata);
        Map<String, Object> _commands = (Map) metadata.get("commands");
        Map<String, Object> _parameters = (Map) metadata.get("parameters");
        for (Object _name: _commands.keySet()) {
            String name = (String) _name;
            String displayName = getString((Map)_commands.get(name), "display_name");
            ProtocolCommand command = new ProtocolCommand(name, displayName);
            commandMetadata.put(name, command);
        }

        // parameters={SampleInterval={set_timeout=10, visibility=READ_WRITE, get_timeout=10, startup=true,
        // direct_access=false, display_name=Polled Interval, value={default=5, units=s, type=int}}},
        for (Object _name: _parameters.keySet()) {
            String name = (String) _name;
            Map param = (Map) _parameters.get(name);
            log.debug("Parameter metadata, name = {}, {}", name, param);
            String displayName = getString(param, "display_name");
            if (displayName.equals(""))
                displayName = name;
            String visibility = getString(param, "visibility");
            String description = getString(param, "description");
            String startup = getString(param, "startup");
            String directAccess = getString(param, "direct_access");

            Map value = (Map) param.get("value");
            String valueDescription = getString(value, "description");
            String valueType = getString(value, "type");
            String units = getString(value, "units");
            Parameter paramObj = new Parameter(name, displayName, description, visibility, startup, directAccess,
                    valueDescription, valueType, units);
            parameterMetadata.put(name, paramObj);
        }
        log.debug("Parsed metadata: commands = {}", commandMetadata);
        log.debug("Parsed metadata: parameters = {}", parameterMetadata);
    }

    public void parseCapabilities(List capes) {
        log.debug("parse capabilities, clearing commandList");
        commandList.clear();
        setParamsSettable(false);
        for (Object cape : capes) {
            log.debug("CAPABILITY: {}", cape);
            String capability = (String) cape;
            log.debug("Found capability: " + capability);
            ProtocolCommand command = commandMetadata.get(capability);
            if (command == null) {
                command = new ProtocolCommand(capability, "");
                commandMetadata.put(capability, command);
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

    public void setParams(Map params) {
        // TODO handle incomplete parameter lists?
        log.debug("setParams, clearing parameterList");
        if (params != null) {
            paramList.clear();
            for (Object key : params.keySet()) {
                String name = (String) key;
                String value = getString(params, name);
                if (name != null) {
                    Parameter param = parameterMetadata.get(name);
                    if (param != null) {
                        if (!Objects.equals(param.getValue(), value)) {
                            log.debug("UPDATED PARAM: " + name + " VALUE: " + value);
                            param.setValue(value);
                        }
                        paramList.add(param);
                    }
                }
            }
        }
    }

    protected void publishSample(DataStream sample) {
        Platform.runLater(()->{
            if (!sampleLists.containsKey(sample.name)) {
                sampleLists.put(sample.name, FXCollections.observableArrayList(new ArrayList<>()));
                sampleTypes.add(sample.name);
            }
            try {
                List<DataStream> samples = sampleLists.get(sample.name);
                if (samples != null) samples.add(sample);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Map<String, Object> getCoefficients() {
        return coefficients;
    }

    public void updateCoefficients(Map<String, Object> coefficients) {
        this.coefficients.putAll(coefficients);
    }

    public void updateCoefficients(File file) throws IOException {
        Reader in = new FileReader(file);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
        for (CSVRecord record: records) {
            try {
                String name = record.get(1);
                String value = record.get(2);
                log.debug("Found coefficient {} : {}", name, value);
                coefficients.put(name, value);
            } catch (ArrayIndexOutOfBoundsException ignore) { }
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

    public DriverConfig getConfig() {
        return config;
    }

    public void setConfig(DriverConfig config) {
        this.config = config;
    }
}
