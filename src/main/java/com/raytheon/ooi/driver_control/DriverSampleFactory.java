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
import java.util.concurrent.ConcurrentHashMap;

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
    private static Map<String, String> coefficients = new HashMap<>();

    static {
        coefficients.put("CC_tc_slope", "0.0000422");
        coefficients.put("CC_ts_slope", "0.003");
        coefficients.put("CC_offset", "2008");
        coefficients.put("CC_gain", "4.00");

        coefficients.put("CC_arr_tac", "[0.0, 0.0, -2.80979E-09, 2.21477E-06, -5.53586E-04, 5.723E-02]");
        coefficients.put("CC_arr_agcl", "[0.0, -8.61134E-10, 9.21187E-07, -3.7455E-04, 6.6550E-02, -4.30086]");
        coefficients.put("CC_e2l_ysz", "[0.0, 0.0, 0.0, 0.0, 1.0, -0.00375]");
        coefficients.put("CC_arr_hgo", "[0.0, 0.0, 4.38978E-10, -1.88519E-07, -1.88232E-04, 9.23720E-01]");
        coefficients.put("CC_arr_tbc1", "[0.0, 0.0, -6.59572E-08, 4.52831E-05, -1.204E-02, 1.70059]");
        coefficients.put("CC_arr_tbc2", "[0.0, 0.0, 8.49102E-08, -6.20293E-05, 1.485E-02, -1.41503]");
        coefficients.put("CC_arr_tbc3", "[-1.86747E-12, 2.32877E-09, -1.18318E-06, 3.04753E-04, -3.956E-02, 2.2047]");
        coefficients.put("CC_arr_agclref", "[0.0, 0.0, -2.5E-10, -2.5E-08, -2.5E-06, -9.025E-02]");
        coefficients.put("CC_e2l_h2", "[0.0, 0.0, 0.0, 0.0, 1.0, -0.00375]");
        coefficients.put("CC_e2l_hs", "[0.0, 0.0, 0.0, 0.0, 1.0, -0.00350]");
        coefficients.put("CC_e2l_agcl", "[0.0, 0.0, 0.0, 0.0, 1.0, -0.00225]");
        coefficients.put("CC_arr_logkfh2g", "[0.0, 0.0, -1.51904000E-07, 1.16655E-04, -3.435E-02, 6.32102]");
        coefficients.put("CC_arr_eh2sg", "[0.0, 0.0, 0.0, 0.0, -4.49477E-05, -1.228E-02]");
        coefficients.put("CC_arr_yh2sg", "[2.3113E+01, -1.8780E+02, 5.9793E+02, -9.1512E+02, 6.7717E+02, -1.8638E+02]");
        coefficients.put("CC_e2l_b", "[0.0, 0.0, 0.0, 0.0, 1.04938, -275.5]");
        coefficients.put("CC_l2s_b", "[0.0, 0.0, 8.7755e-08, 0.0, 0.000234101, 0.001129306]");
        coefficients.put("CC_e2l_r", "[0.0, 0.0, 0.0, 0.0, 1.04938, -275.5]");
        coefficients.put("CC_l2s_r", "[0.0, 0.0, 8.7755e-08, 0.0, 0.000234101, 0.001129306]");
        coefficients.put("CC_e2l_L", "[0.0, 0.0, 0.0, 0.0, 0.9964, -0.46112]");
        coefficients.put("CC_l2s_L", "[9.32483e-7, -0.000122268, 0.00702, -0.23532, 17.06172, 0.0]");
        coefficients.put("CC_e2l_H", "[0.0, 0.0, 0.0, 0.0, 0.9979, -0.10287]");
        coefficients.put("CC_l2s_H", "[9.32483e-7, -0.000122268, 0.00702, -0.23532, 17.06172, 0.0]");
        coefficients.put("CC_s2v_r", "[5.83124e-14, -4.09038e-11, -3.44498e-8, 5.14528e-5, 0.05841, 0.00209]");
    }

    private DriverSampleFactory() {
    }

    public static Map<String, Object> parseSample(String s, PreloadDatabase db) {
        Map<String, Object> map = new ConcurrentHashMap<>();
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
                        JSONObject functionMap = new JSONObject(p.getParameterFunctionMap());
                        Map<String, String> args = new HashMap<>();

                        for (Object o : functionMap.keySet()) {
                            String key = (String) o;
                            String name = functionMap.getString(key);
                            if (coefficients.containsKey(name)) {
                                args.put(key, coefficients.get(name));
                            } else {
                                String argName = db.getParameterById(name).getName();
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
                            Number calculatedValue = applyFunction(db.getParameterFunctionById(p.getParameterFunctionId()), args);
                            if (counter == 0)
                                map.put("*" + p.getName(), calculatedValue);
                            else
                                map.put("**" + p.getName(), calculatedValue);
                        }
                    });
        }

        writeData(map, params);
        return map;
    }

    public static void writeData(Map<String, Object> map, Map<String, DataParameter> params) {

    }

    public static Number applyFunction(DataFunction df, Map<String, String> args) {
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
            writer.append(String.format("from %s import %s\n", df.getOwner(), df.getFunction()));
            // build the function inputs
            for (String key : args.keySet()) {
                writer.append(String.format("%s = %s\n", key, args.get(key)));
            }
            writer.append(String.format("print %s(%s)\n", df.getFunction(), joiner.toString()));
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
