package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.preload.PreloadDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class DriverSampleFactory {
    public static final String STREAM_NAME = "stream_name";
    public static final String QUALITY_FLAG = "quality_flag";
    public static final String PREFERRED_TIMESTAMP = "preferred_timestamp";
    public static final String PORT_TIMESTAMP = "port_timestamp";
    public static final String DRIVER_TIMESTAMP = "driver_timestamp";
    public static final String PKT_FORMAT_ID = "pkt_format_id";
    public static final String PKT_VERSION = "pkt_version";
    public static final String VALUE = "value";
    public static final String VALUES = "values";
    public static final String VALUE_ID = "value_id";
    private static Logger log = LogManager.getLogger("DriverSampleFactory");


    // temp storage for coefficients
    private static Map<String, Number> coefficients = new HashMap<>();
    static {
        coefficients.put("CC_tc_slope", 0.0000422);
        coefficients.put("CC_ts_slope", 0.003);
        coefficients.put("CC_offset", 2008);
        coefficients.put("CC_gain", 4.00);
    }


    private DriverSampleFactory() {}

    public static Map<String, Object> parseSample(String s, PreloadDatabase db) {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject(s);
        JSONArray json_values =  json.getJSONArray(VALUES);

        map.put(STREAM_NAME, json.getString(STREAM_NAME));
        map.put(PREFERRED_TIMESTAMP, json.getString(PREFERRED_TIMESTAMP));
        map.put(QUALITY_FLAG, json.getString(QUALITY_FLAG));
        map.put(PORT_TIMESTAMP, json.getDouble(PORT_TIMESTAMP));
        map.put(DRIVER_TIMESTAMP, json.getDouble(DRIVER_TIMESTAMP));
        map.put(PKT_FORMAT_ID, json.getString(PKT_FORMAT_ID));
        map.put(PKT_VERSION, json.getInt(PKT_VERSION));

        for (int i=0; i<json_values.length(); i++) {
            JSONObject element = (JSONObject) json_values.get(i);
            map.put(element.getString(VALUE_ID), element.get(VALUE));
        }

        DataStream stream = db.getStream(json.getString(STREAM_NAME));
        Map<String, DataParameter> params = stream.getParams();
        for (DataParameter param: params.values()) {
            String PFID = param.getParameterFunctionId();
            if (PFID != null) {
                JSONObject funcmap = new JSONObject(param.getParameterFunctionMap());
                Map<String, Number> args = new HashMap<>();
                for (Object o: funcmap.keySet()) {
                    String key = (String) o;
                    String name = funcmap.getString(key);
                    Number value;
                    if (coefficients.containsKey(name)) {
                        args.put(key, coefficients.get(name));
                    } else {
                        String argname = db.getParameterById(name).getName();
                        args.put(key, (Number) map.get(argname));
                    }
                }
                Number calculatedValue = applyFunction(db.getParameterFunctionById(PFID), args);
                log.debug("Calculated value: {}", calculatedValue);
                map.put(param.getName(), calculatedValue);
            }
        }

        return map;
    }

    public static Number applyFunction(DataFunction df, Map<String, Number> args) {
        StringJoiner joiner = new StringJoiner(", ");
        JSONArray functionArgs = new JSONArray(df.getArgs());
        for (int i=0; i<functionArgs.length(); i++) {
            String argName = functionArgs.getString(i);
            log.debug("index: {} argName: {} args: {} value: {}", i, argName, args, args.get(argName));
            joiner.add(argName);
        }

        try {
            Path ion_function = Files.createTempFile("ion_function", ".py");
            FileWriter writer = new FileWriter(ion_function.toFile());
            // import numpy
            writer.append("import numpy\n");
            // import the correct function
            writer.append(String.format("from %s import %s\n", df.getOwner(), df.getFunction()));
            // build the function inputs
            for (String key: args.keySet()) {
                writer.append(String.format("%s = numpy.array([%s])\n", key, args.get(key)));
            }
            writer.append(String.format("print %s(%s)\n", df.getFunction(), joiner.toString()));
            writer.close();
            log.debug("ION_FUNCTION: {}", ion_function);
            String[] command = {"python", ion_function.toString()};

            ProcessBuilder pb = new ProcessBuilder(command);
            Map<String, String> environment = pb.environment();
            environment.putAll(DriverLauncher.getEnv("."));
            log.debug(environment);

            Process p = pb.start();
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader br2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (br2.ready()) {
                log.debug("ERROR FROM PYTHON: {}", br2.readLine());
            }
            String line = br.readLine();
            if (line == null) {
                log.debug("No response from ion_functions...");
                return 0;
            }
            JSONArray rvalue = new JSONArray(line);
            return (Number) rvalue.get(0);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return 0; //Double.parseDouble(line);
    }
}
