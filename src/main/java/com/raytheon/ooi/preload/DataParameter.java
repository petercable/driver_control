package com.raytheon.ooi.preload;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.driver_control.DriverLauncher;
import com.raytheon.ooi.driver_control.DriverModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class DataParameter {
    private String id;
    private String name;
    private String parameterType;
    private String valueEncoding;
    private String parameterFunctionId;
    private String parameterFunctionMap;
    private Object value;
    private DataStream stream;
    private boolean isDummy = false;
    private final DriverModel model = DriverModel.getInstance();
    private static Logger log = LogManager.getLogger(DataParameter.class);
    private final PreloadDatabase preload = SqlitePreloadDatabase.getInstance();
    Map<String, String> coefficients = model.getCoefficients();

    public DataParameter(String id, String name, String parameterType, String valueEncoding,
                         String parameterFunctionId, String parameterFunctionMap) {
        this.id = id;
        this.name = name;
        this.parameterType = parameterType;
        this.valueEncoding = valueEncoding;
        this.parameterFunctionId = parameterFunctionId;
        this.parameterFunctionMap = parameterFunctionMap;
    }

    public String getName() {
        return name;
    }

    public String getParameterType() {
        return parameterType;
    }

    public String getValueEncoding() {
        return valueEncoding;
    }

    public String getParameterFunctionId() {
        return parameterFunctionId;
    }

    public String getParameterFunctionMap() {
        return parameterFunctionMap;
    }

    public boolean getIsDummy() {
        return isDummy;
    }

    public String toString() {
        return String.format("ID: %s NAME: %s TYPE: %s ENCODING: %s FUNCID: %s FUNCMAP: %s",
                id,
                name,
                parameterType,
                valueEncoding,
                parameterFunctionId,
                parameterFunctionMap);
    }

    public String getId() {
        return id;
    }

    public Object getValue() {
        // do something here
        if (parameterType.equals(Constants.PARAMETER_TYPE_FUNCTION) && value == null) {
            value = calculateValue();
        }
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object calculateValue() {
        // decode the function map
        JSONObject functionMap = (JSONObject) JSONValue.parse(parameterFunctionMap.replace("'", "\""));
        log.debug("FunctionMap: {}", functionMap);

        // build an args map
        Map<String, String> args = new HashMap<>();

        for (Object o : functionMap.keySet()) {
            String key = (String) o;
            String name = (String) functionMap.get(key);
            if (coefficients.containsKey(name)) {
                args.put(key, coefficients.get(name));
            } else if (stream.containsParam(name)) {
                args.put(key, (String) stream.getParamValue(name));
            } else {
                isDummy = true;
                args.put(key, "0");
            }
        }
        if (args.size() > 0) {
            return applyFunction(preload.getParameterFunctionById(parameterFunctionId), args);
        }
        return null;
    }

    public void setStream(DataStream stream) {
        this.stream = stream;
    }

    public static Object applyFunction(DataFunction df, Map<String, String> args) {
        StringJoiner joiner = new StringJoiner(", ");
        JSONArray functionArgs = (JSONArray) JSONValue.parse(df.getArgs().replace("'", "\""));
        for (int i = 0; i < functionArgs.size(); i++) {
            String argName = (String) functionArgs.get(i);
            log.debug("index: {} argName: {} value: {}", i, argName, args.get(argName));
            joiner.add(argName);
        }

        try {
            Path ion_function = Files.createTempFile("ion_function", ".py");
            FileWriter writer = new FileWriter(ion_function.toFile());
            // import numpy
            writer.append("import numpy\n");
            // import the correct function
            if (df.getOwner() != null)
                writer.append(String.format("from %s import %s\n", df.getOwner(), df.getFunction()));
            // build the function inputs
            for (String key : args.keySet()) {
                String value = args.get(key);
                // check and see if the value is already a list
                // if not, make it a list and wrap the list in numpy.array
                // this is a workaround to ion_functions expecting lists of data
                // rather than one record at a time.
                if (!value.startsWith("[")) value = "[" + value + "]";
                writer.append(String.format("%s = numpy.array(%s)\n", key, value));
            }
            if (df.getOwner() != null)
                writer.append(String.format("print %s(%s)\n", df.getFunction(), joiner.toString()));
            else
                writer.append(String.format("print %s\n", df.getFunction()));
            writer.close();
            log.debug("ION_FUNCTION: {}", ion_function);
            String[] command = {"python", ion_function.toString()};

            ProcessBuilder pb = new ProcessBuilder(command);
            Map<String, String> environment = pb.environment();
            environment.putAll(DriverLauncher.getEnv("."));

            Process p = pb.start();
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader br2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (br2.ready()) {
                log.debug("ERROR FROM PYTHON: {}", br2.readLine());
            }
            String line = br.readLine();
            if (line == null || !line.startsWith("[")) {
                log.debug("No response from ion_functions...");
                return 0;
            }
            log.debug(line);
            JSONArray rvalue = (JSONArray) JSONValue.parse(line);
            if (rvalue == null)
                return 0;
            return rvalue.get(0);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return 0;
    }
}
