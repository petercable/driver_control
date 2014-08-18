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
import java.math.BigInteger;
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
        String className = "Null";
        if (value != null)
            className = value.getClass().toString();
        return String.format("ID: %s NAME: %s VALUE: %s VALUE_CLASS: %s TYPE: %s ENCODING: %s FUNCID: %s FUNCMAP: %s",
                id,
                name,
                getValue(),
                className,
                parameterType,
                valueEncoding,
                parameterFunctionId,
                parameterFunctionMap);
    }

    public String getId() {
        return id;
    }

    public synchronized Object getValue() {
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
                log.debug("DataParameter::calculateValue - using dummy value for {} {}", name, key);
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
    
    public void validate() {
        if (value == null)
            log.error("Missing required value from stream: {}", this);
        switch (parameterType) {
            case Constants.PARAMETER_TYPE_QUANTITY:
            case Constants.PARAMETER_TYPE_FUNCTION:
                validateType(getValue());
                break;
            case Constants.PARAMETER_TYPE_ARRAY:
                if (getValue() instanceof String) {
                    Object array = JSONValue.parse((String) getValue());
                    if (array instanceof JSONArray) {
                        log.debug("YAHOO, found JSONArray: {}", array);
                    }
                else if (getValue() instanceof Byte[]) {
                        log.debug("Found byte array: {}", getValue());
                    }
                } else {
                    log.debug("Found some other sort of object: {} {}", getValue(), getValue().getClass().toString());
                }
                break;
            case Constants.PARAMETER_TYPE_BOOLEAN:
                if (!(getValue() instanceof Boolean))
                    log.error("UNEXPECTED VALUE {} for parameter type {} for parameter {}", getValue(), parameterType, name);
                break;
            case Constants.PARAMETER_TYPE_CATEGORY:
                log.debug("YAHOO, found {}: {}", parameterType, getValue());
                break;
            default:
                log.error("Missing parameterType from switch statement in validate: {}", this);
        }
    }
        
    public boolean validateType(Object thisValue) {
        try{
            if (thisValue == null) return false;
            Long longValue;
            Double doubleValue;
            boolean badLong = false;
            if (thisValue instanceof Integer) {
                longValue = ((Integer) thisValue).longValue();
                doubleValue = ((Integer) thisValue).doubleValue();
            } else if (thisValue instanceof Long) {
                longValue = (Long) thisValue;
                doubleValue = ((Long) thisValue).doubleValue();
            } else if (thisValue instanceof Double) {
                doubleValue = (Double) thisValue;
                longValue = 0l;
                badLong = true;
            } else {
                log.debug("Non-numeric type for this parameter: {}", this.toString());
                return true;
            }

            switch (valueEncoding) {
                // INTS
                case Constants.VALUE_TYPE_INT8:
                    if (longValue > Byte.MAX_VALUE || longValue < Byte.MIN_VALUE || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_INT16:
                    if (longValue > Short.MAX_VALUE || longValue < Short.MIN_VALUE || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_INT32:
                    if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_INT64:
                    if (longValue > Long.MAX_VALUE || longValue < Long.MIN_VALUE || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                // FLOATS
                case Constants.VALUE_TYPE_FLOAT32:
                    if (doubleValue > Float.MAX_VALUE || doubleValue < -Float.MAX_VALUE)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_FLOAT64:
                    if (doubleValue > Double.MAX_VALUE || doubleValue < -Double.MAX_VALUE)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                // UINTS
                case Constants.VALUE_TYPE_UINT8:
                    if (longValue > (Byte.MAX_VALUE*2) || longValue < (Byte.MIN_VALUE*2) || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_UINT16:
                    if (longValue > (Short.MAX_VALUE*2) || longValue < (Short.MIN_VALUE*2) || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_UINT32:
                    if (longValue > (Long.MAX_VALUE) || longValue < Long.MIN_VALUE || badLong)
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    break;
                case Constants.VALUE_TYPE_UINT64:
                    //TODO || (Integer) value < Byte.MIN_VALUE)
                    BigInteger int1 = new BigInteger((String)thisValue);
                    if(int1.compareTo(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2))) == 1){
                        log.error("VALUE OVERFLOW: value {} found in {} parameter {}", thisValue, valueEncoding, name);
                    }
                    break;
                default:
                    log.error("UNEXPECTED VALUE TYPE {} for parameter type {} for parameter {}", valueEncoding, parameterType, name);
                    log.error("Parameter: {}", this.toString());
            }
        } catch (NumberFormatException | ClassCastException e){
            e.printStackTrace();
            log.error("Expected VALUE TYPE {}, received VALUE {} for parameter {}", valueEncoding, thisValue, name);
        }
        return true;
    }
}
