package com.raytheon.ooi.preload;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.common.JsonHelper;
import com.raytheon.ooi.driver_control.DriverModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.raytheon.ooi.common.JsonHelper.toList;

public class DataParameter {
    private final DriverModel model = DriverModel.getInstance();
    private final PreloadDatabase preload = SqlitePreloadDatabase.getInstance();
    private final static Logger log = LoggerFactory.getLogger(DataParameter.class);

    public final String id;
    public final String name;
    public final String parameterType;
    public final String valueEncoding;
    public final String parameterFunctionId;
    public final String parameterFunctionMap;
    private Path functionDir;

    private Object value;
    private DataStream stream;
    private boolean isDummy = false;
    private boolean failedValidate = false;
    private boolean missing = false;

    public DataParameter(String id, String name, String parameterType, String valueEncoding,
                         String parameterFunctionId, String parameterFunctionMap) {
        this.id = id;
        this.name = name;
        this.parameterType = parameterType;
        this.valueEncoding = valueEncoding;
        this.parameterFunctionId = parameterFunctionId;
        this.parameterFunctionMap = parameterFunctionMap;

        functionDir = Paths.get(model.getConfig().getScenarioDir(), "functions");
        if (!Files.isDirectory(functionDir))
            try {
                Files.createDirectory(functionDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public boolean getIsDummy() {
        return isDummy;
    }

    public String toString() {
        String className = "Null";
        Object thisValue = getValue();
        if (thisValue != null)
            className = value.getClass().toString();

        return String.format("ID: %s NAME: %s VALUE: %s VALUE_CLASS: %s TYPE: %s ENCODING: %s FUNCID: %s FUNCMAP: %s",
                id,
                name,
                thisValue,
                className,
                parameterType,
                valueEncoding,
                parameterFunctionId,
                parameterFunctionMap);
    }

    public synchronized Object getValue() {
        if (stream == null)
            // stream not yet defined, we can't calculate squat
            return value;
        if (parameterType.equals(Constants.PARAMETER_TYPE_FUNCTION) && value == null) {
            try {
                value = calculateValue();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object calculateValue() throws IOException {
        // decode the function map
        Map functionMap = JsonHelper.toMap(parameterFunctionMap.replace("'", "\""));
        log.debug("FunctionMap: {}", functionMap);

        // build an args map
        Map<String, Object> args = new HashMap<>();

        for (Object o : functionMap.keySet()) {
            String key = (String) o;
            String id = (String) functionMap.get(key);
            String paramName = preload.getParameterName(id);
            if (model.getCoefficients().containsKey(id)) {
                args.put(key, model.getCoefficients().get(id));
            } else if (stream.containsParam(paramName)) {
                args.put(key, stream.getParamValue(paramName));
            } else {
                log.debug("DataParameter::calculateValue - using dummy value for {} {} {}", key, id, paramName);
                isDummy = true;
                args.put(key, 0);
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

    public Object applyFunction(DataFunction df, Map<String, Object> args) throws IOException {
        StringJoiner joiner = new StringJoiner(", ");
        List functionArgs = toList(df.getArgs().replace("'", "\""));
        for (int i = 0; i < functionArgs.size(); i++) {
            String argName = (String) functionArgs.get(i);
            log.debug("index: {} argName: {} value: {}", i, argName, args.get(argName));
            joiner.add(argName);
        }

        try {

            Path ion_function = Files.createTempFile(functionDir, String.format("ion_function_%s_", df.getFunction()), ".py");
            FileWriter writer = new FileWriter(ion_function.toFile());
            // import numpy
            writer.append("import numpy\n");
            writer.append("import json\n");
            // import the correct function
            if (df.getOwner() != null)
                writer.append(String.format("from %s import %s\n", df.getOwner(), df.getFunction()));
            // build the function inputs
            for (String key : args.keySet()) {
                Object value = args.get(key);
                String valueString;
                // check and see if the value is already a list
                // if not, make it a list and wrap the list in numpy.array
                // this is a workaround to ion_functions expecting lists of data
                // rather than one record at a time.
                if (value instanceof String) {
                    valueString = (String) value;
                    if (!valueString.startsWith("["))
                        value = "['" + value + "']";
                } else if (!(value instanceof List)) {
                    value = String.format("[%s]", value);
                }
                writer.append(String.format("%s = numpy.array(%s)\n", key, value));
            }
            if (df.getOwner() != null)
                writer.append(String.format("result = %s(%s)\n", df.getFunction(), joiner.toString()));
            else
                writer.append(String.format("result = %s\n", df.getFunction()));
            writer.append("print json.dumps(result.tolist())\n");
            writer.close();
            log.debug("ION_FUNCTION: {}", ion_function);
            String[] command = {"python", ion_function.toString()};

            ProcessBuilder pb = new ProcessBuilder(command);
            Map<String, String> environment = pb.environment();

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
            log.debug("Read from ion_function: {}", line);
            List rvalue = toList(line);

            log.debug("Parsed result from ion_function: {}", rvalue);
            if (rvalue == null)
                return 0;
            return rvalue.get(0);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return 0;
    }
    
    public void validate() {
        Object thisValue = getValue();
        if (thisValue == null)
            log.error("Missing required value from stream: {}", this);
        switch (parameterType) {
            case Constants.PARAMETER_TYPE_QUANTITY:
            case Constants.PARAMETER_TYPE_FUNCTION:
                validateType(thisValue);
                break;
            case Constants.PARAMETER_TYPE_ARRAY:
                if (thisValue instanceof String) {
                    List array = null;
                    try {
                        array = toList((String) thisValue);
                        if (array != null)
                            log.debug("YAHOO, found JSONArray: {}", array);
                    } catch (IOException ignored) {
                        log.error("Found bare string in array field: {}", thisValue);
                    }

                }
                else if (thisValue instanceof Byte[]) {
                        log.debug("Found byte array: {}", thisValue);
                } else {
                    Object classType = null;
                    if (thisValue != null) classType = thisValue.getClass();
                    log.debug("Found some other sort of object: {} {}", value, classType);
                }
                break;
            case Constants.PARAMETER_TYPE_BOOLEAN:
                if (!(thisValue instanceof Boolean))
                    log.error("UNEXPECTED VALUE {} for parameter type {} for parameter {}", thisValue, parameterType, name);
                break;
            case Constants.PARAMETER_TYPE_CATEGORY:
                log.debug("YAHOO, found {}: {}", parameterType, thisValue);
                break;
            default:
                log.error("Missing parameterType from switch statement in validate: {}", this);
        }
    }
        
    public void validateType(Object thisValue) {
        if (thisValue == null) return;
        Long longValue = 0l;
        Double doubleValue = 0.0;
        boolean badLong = false;
        if (thisValue instanceof Integer) {
            longValue = ((Integer) thisValue).longValue();
            doubleValue = ((Integer) thisValue).doubleValue();
        } else if (thisValue instanceof Long) {
            longValue = (Long) thisValue;
            doubleValue = ((Long) thisValue).doubleValue();
        } else if (thisValue instanceof Double) {
            doubleValue = (Double) thisValue;
            badLong = true;
        } else if (thisValue instanceof BigInteger) {
            badLong = true;
        } else {
            log.debug("Non-numeric type for this parameter: {} {}", this, thisValue);
            return;
        }

        switch (valueEncoding) {
            // INTS
            case Constants.VALUE_TYPE_INT8:
                if (badLong || longValue > Byte.MAX_VALUE || longValue < Byte.MIN_VALUE)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_INT16:
                if (badLong || longValue > Short.MAX_VALUE || longValue < Short.MIN_VALUE)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_INT32:
                if (badLong ||longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_INT64:
                if (badLong || longValue > Long.MAX_VALUE || longValue < Long.MIN_VALUE )
                    failedValidate = true;
                break;
            // FLOATS
            case Constants.VALUE_TYPE_FLOAT32:
                if (doubleValue > Float.MAX_VALUE || doubleValue < -Float.MAX_VALUE)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_FLOAT64:
                if (doubleValue > Double.MAX_VALUE || doubleValue < -Double.MAX_VALUE)
                    failedValidate = true;
                break;
            // UINTS
            case Constants.VALUE_TYPE_UINT8:
                if (badLong || longValue > (Byte.MAX_VALUE*2+1) || longValue < 0)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_UINT16:
                if (badLong || longValue > (Short.MAX_VALUE*2+1) || longValue < 0)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_UINT32:
                // TODO -- is MAX UINT32 Long.MAX_VALUE + 1??
                if (badLong || longValue < 0)
                    failedValidate = true;
                break;
            case Constants.VALUE_TYPE_UINT64:
                BigInteger big;
                if (value instanceof Double) {
                    failedValidate = true;
                    break;
                } else if (value instanceof Integer) {
                    big = BigInteger.valueOf(((Integer) value).longValue());
                } else if (value instanceof Long) {
                    big = BigInteger.valueOf((Long)value);
                } else if (value instanceof BigInteger) {
                    big = (BigInteger) value;
                } else {
                    failedValidate = true;
                    break;
                }

                BigInteger tooBig = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2l)).add(BigInteger.ONE);
                if (big.compareTo(tooBig) == 1 || big.compareTo(BigInteger.ZERO) == -1)
                    failedValidate = true;
                break;
            default:
                failedValidate = true;
        }
        if (failedValidate) {
            log.error(String.format("VALUE OVERFLOW or BAD TYPE: parameter %s : %s", this, thisValue));
        }
    }

    public boolean isFailedValidate() {
        return failedValidate;
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }
}
