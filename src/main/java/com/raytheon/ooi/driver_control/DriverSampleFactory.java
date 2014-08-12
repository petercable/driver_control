package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.preload.PreloadDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

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

    private DriverSampleFactory() {
    }

    public static Map<String, Object> parseSample(String s, PreloadDatabase db, DriverConfig config) {
        Map<String, Object> map = new ConcurrentHashMap<>();
        Map<String, String> coefficients = config.getCoefficients();
        JSONObject json = new JSONObject(s);
        JSONArray json_values = json.getJSONArray(VALUES);

        map.put(STREAM_NAME, json.getString(STREAM_NAME));
        map.put(PREFERRED_TIMESTAMP, json.getString(PREFERRED_TIMESTAMP));
        map.put(QUALITY_FLAG, json.getString(QUALITY_FLAG));
        map.put(PORT_TIMESTAMP, json.getDouble(PORT_TIMESTAMP));
        map.put(DRIVER_TIMESTAMP, json.getDouble(DRIVER_TIMESTAMP));
        map.put(PKT_FORMAT_ID, json.getString(PKT_FORMAT_ID));
        map.put(PKT_VERSION, json.getInt(PKT_VERSION));

        log.trace("Loading instrument supplied values into sample object...");
        for (int i = 0; i < json_values.length(); i++) {
            JSONObject element = (JSONObject) json_values.get(i);
            map.put(element.getString(VALUE_ID), element.get(VALUE));
        }

        DataStream stream = db.getStream(json.getString(STREAM_NAME));
        Map<String, DataParameter> params = stream.getParams();

        // make two passes, L1 data needs to be available before L2 can be calculated
        for (int i = 0; i < 2; i++) {
            final int counter = i;
            params.values()
                    .parallelStream()
                    .filter((p) -> p.getParameterType().equals("function"))
                    .filter((p) -> p.getParameterFunctionId() != null)
                    .filter((p) -> !map.containsKey(p.getName()))
                    .filter((p) -> !map.containsKey("*"+p.getName()))
                    .forEach((p) -> {
                        log.debug("ID: {} Parameter Function Map: {}", p.getId(), p.getParameterFunctionMap());
                        try {
                            JSONObject functionMap = new JSONObject(p.getParameterFunctionMap());
                            Map<String, String> args = new HashMap<>();

                            for (Object o : functionMap.keySet()) {
                                String key = (String) o;
                                String name = functionMap.getString(key);
                                if (coefficients.containsKey(name)) {
                                    args.put(key, coefficients.get(name));
                                } else {
                                    DataParameter dp = db.getParameterById(name);
                                    if (dp == null) {
                                        log.error("UNABLE TO RETRIEVE PARAMETER: {}", name);
                                        args.clear();
                                        break;
                                    }
                                    String argName = dp.getName();
                                    log.debug(argName);
                                    if (map.containsKey(argName))
                                        args.put(key, map.get(argName).toString());
                                    else if (map.containsKey("*" + argName))
                                        args.put(key, map.get("*" + argName).toString());
                                    else {
                                        log.error("Could not find parameter key: {} name: {} argName: {}", key, name, argName);
                                        if (counter == 0) {
                                            args.clear();
                                            break;
                                        }
                                        args.put(key, "0");
                                    }
                                }
                            }
                            if (args.size() > 0) {
                                Object calculatedValue = applyFunction(db.getParameterFunctionById(p.getParameterFunctionId()), args);
                                if (counter == 0)
                                    map.put("*" + p.getName(), calculatedValue);
                                else
                                    map.put("**" + p.getName(), calculatedValue);
                            }
                        } catch (Exception e) {
                            log.error("Exception parsing function map: {}", e);
                        }
                    });
        }
        writeData(map, params, config);
        return map;
    }

    public static void writeData(Map<String, Object> map, Map<String, DataParameter> params, DriverConfig config) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        Path outputFile = Paths.get(config.getTemp(), config.getScenario(), map.get(STREAM_NAME) + ".csv");
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
                if (map.containsKey(name))
                    joiner.add(map.get(name).toString());
                else if (map.containsKey("*"+name))
                    joiner.add(map.get("*"+name).toString());
                else if (map.containsKey("**"+name))
                    joiner.add(map.get("**"+name).toString());
                else
                    joiner.add("");
            });
            out.write(joiner.toString().getBytes());
            out.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object applyFunction(DataFunction df, Map<String, String> args) {
        StringJoiner joiner = new StringJoiner(", ");
        JSONArray functionArgs = new JSONArray(df.getArgs());
        for (int i = 0; i < functionArgs.length(); i++) {
            String argName = functionArgs.getString(i);
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
            JSONArray rvalue = new JSONArray(line);
            return rvalue.get(0);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return 0;
    }
}
