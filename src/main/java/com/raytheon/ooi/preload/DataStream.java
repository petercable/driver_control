package com.raytheon.ooi.preload;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.driver_control.DriverModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class DataStream {
    public final String name;
    private final DriverModel model = DriverModel.getInstance();
    private final Map<String, DataParameter> params = new HashMap<>();
//    private final Map<String, Object> metadata = new HashMap<>();
    private final static Logger log = LogManager.getLogger(DataStream.class);

    public DataStream(String name) {
        this.name = name;
    }

    public Map<String, DataParameter> getParams() {
        return params;
    }

    public String toString() {
        return String.format("DataStream(%s)", name);
    }

    public void setValues(Map<String, Object> values) {
        log.debug("DataStream::setValues: {}", values);
        for (String name: values.keySet()) {
            if (params.containsKey(name)) {
                params.get(name).setValue(values.get(name));
            } else {
                switch (name) {
                    case Constants.STREAM_NAME:
                    case Constants.PKT_FORMAT_ID:
                    case Constants.PKT_VERSION:
                    case Constants.QUALITY_FLAG:
                        break;
                    default:
                        log.error("Parameter in stream not present in preload stream definition! {}:{}", name, values.get(name));
                        break;
                }
            }
        }
    }

    public boolean containsParam(String paramName) {
        return params.containsKey(paramName);
    }

    public Object getParamValue(String paramName) {
        if (!params.containsKey(paramName)) return null;
        return params.get(paramName).getValue();
    }

    public Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<>();
        for (String key: params.keySet())
            values.put(key, params.get(key).getValue());

//        for (String key: metadata.keySet())
//            values.put(key, metadata.get(key));
        return values;
    }

    public void validate() {
        params.values().stream().forEach(DataParameter::validate);
    }

    public void archive() {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        Path outputFile = Paths.get(model.getConfig().getTemp(), model.getConfig().getScenario(), name + ".csv");
        boolean writeHeader = false;
        if (!Files.exists(outputFile))
            writeHeader = true;
        try (OutputStream out = Files.newOutputStream(outputFile, CREATE, APPEND)) {
            if (writeHeader) {
                StringJoiner joiner = new StringJoiner(",");
                names.stream().forEach(joiner::add);
                out.write(joiner.toString().getBytes());
                out.write('\n');
            }
            StringJoiner joiner = new StringJoiner(",");
            names.stream().forEach((name)-> {
                if (params.containsKey(name)) {
                    DataParameter dataParameter = params.get(name);
                    if (dataParameter == null) {
                        log.error("Unable to retrieve parameter: {}", name);
                        joiner.add("");
                    } else {
                        Object value = dataParameter.getValue();
                        if (value == null) {
                            log.error("Retrieved null value from dataParameter: {}", name);
                            joiner.add("");
                        } else {
                            joiner.add((value.toString()));
                        }
                    }
                } else {
                    joiner.add("");
                }
            });
            out.write(joiner.toString().getBytes());
            out.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
