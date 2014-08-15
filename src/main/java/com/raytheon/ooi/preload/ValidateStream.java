package com.raytheon.ooi.preload;

import com.raytheon.ooi.driver_control.DriverModel;
import com.raytheon.ooi.driver_control.DataStream;
import org.apache.logging.log4j.LogManager;

import java.math.BigInteger;
import java.util.Map;

public class ValidateStream {

    private PreloadDatabase preload;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    private final String PARAMETER_TYPE_QUANTITY = "quantity";
    private final String PARAMETER_TYPE_FUNCTION = "function";
    private final String PARAMETER_TYPE_ARRAY = "array<quantity>";
    private final String PARAMETER_TYPE_CATEGORY = "category<int8:str>";
    private final String PARAMETER_TYPE_BOOLEAN = "boolean";

    private final String VALUE_TYPE_INT8 = "int8";
    private final String VALUE_TYPE_INT16 = "int16";
    private final String VALUE_TYPE_INT32 = "int32";
    private final String VALUE_TYPE_INT64 = "int64";

    private final String VALUE_TYPE_UINT8 = "uint8";
    private final String VALUE_TYPE_UINT16 = "uint16";
    private final String VALUE_TYPE_UINT32 = "uint32";
    private final String VALUE_TYPE_UINT64 = "uint64";

    private final String VALUE_TYPE_FLOAT32 = "float32";
    private final String VALUE_TYPE_FLOAT64 = "float64";

    private final String VALUE_TYPE_STR = "str";


    public static void main(String[] args){
        ValidateStream vs = new ValidateStream();

        //vs.verifyValueEncoding();


    }



    public void validateStreams(PreloadDatabase preload, DriverModel model, String scenario) {

        //TODO - NEEDS TO DO ALL SAMPLES!

//        findExtraParams();
//
//        if(doesParameterExistInSample()){
//            verifyValueEncoding();
//        }
    }


    public void verifyValueEncoding(String parameterType, String valueType, Object value, String paramName){


        if(parameterType.equals(PARAMETER_TYPE_QUANTITY) || parameterType.equals(PARAMETER_TYPE_FUNCTION)){
            try{
                switch(valueType) {
                    case VALUE_TYPE_INT8:
                        if ((Integer) value > Byte.MAX_VALUE | (Integer) value < Byte.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_INT16:
                        if ((Integer) value > Short.MAX_VALUE | (Integer) value < Short.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;

                    case VALUE_TYPE_INT32:
                        if ((Integer) value > Integer.MAX_VALUE | (Integer) value < Integer.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;

                    case VALUE_TYPE_INT64:
                        if ((Integer) value > Long.MAX_VALUE | (Integer) value < Long.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_FLOAT32:
                        if (value instanceof Integer) {
                            value = ((Integer) value).doubleValue();
                        }
                        if ((Double) value > Double.MAX_VALUE | (Integer) value < Double.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_FLOAT64:
                        if (value instanceof Integer) {
                            value = ((Integer) value).doubleValue();
                        }
                        if ((Double) value > Float.MAX_VALUE | (Integer) value < Float.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_UINT8:
                        if ((Integer) value > (Byte.MAX_VALUE*2) | (Integer) value < (Byte.MIN_VALUE*2))
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_UINT16:
                        if ((Integer) value > (Short.MAX_VALUE*2) | (Integer) value < (Short.MIN_VALUE*2))
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;

                    case VALUE_TYPE_UINT32:
                        if ((Long) value > (Long.MAX_VALUE) | (Integer) value < Long.MIN_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_UINT64:
                        //TODO | (Integer) value < Byte.MIN_VALUE)
                        BigInteger int1 = BigInteger.valueOf((long)value);
                        if(int1.compareTo(BigInteger.valueOf(Long.MAX_VALUE*2)) == 1){
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        }
                        break;
                    default:
                        log.error("UNEXPECTED VALUE TYPE {} for parameter type {}", valueType, parameterType);
                }
            } catch (NumberFormatException e){
                log.error("Expected VALUE TYPE {}, received VALUE {} for parameter {}", valueType, value, paramName);
            }

        } else if(parameterType.equals(PARAMETER_TYPE_ARRAY)){
                switch (valueType) {
                    case VALUE_TYPE_STR:
                        break;

                //#TODO = NEED TO VERIFY EACH VALUE IN ARRAY AS GOOD, PROBABLY USE FOR LOOP
                    case VALUE_TYPE_INT8:
                        if ((Integer) value > Byte.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_INT16:
                        if ((Integer) value > Short.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;

                    case VALUE_TYPE_INT32:
                        if ((Integer) value > Integer.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;

                    case VALUE_TYPE_INT64:
                        if ((Integer) value > Long.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_FLOAT32:
                        if (value instanceof Integer) {
                            value = ((Integer) value).doubleValue();
                        }
                        if ((Double) value > Double.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_FLOAT64:
                        if (value instanceof Integer) {
                            value = ((Integer) value).doubleValue();
                        }
                        if ((Double) value > Float.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_UINT8:
                        if ((Integer) value > (Byte.MAX_VALUE*2))
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_UINT16:
                        if ((Integer) value > (Short.MAX_VALUE*2))
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;

                    case VALUE_TYPE_UINT32:
                        if ((Long) value > (Long.MAX_VALUE))
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    case VALUE_TYPE_UINT64:

                        BigInteger int1 = BigInteger.valueOf((long)value);
                        if(int1.compareTo(BigInteger.valueOf(Long.MAX_VALUE*2)) == 1){
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        }
                        break;
                    default:
                        log.error("UNEXPECTED VALUE TYPE {} for parameter type {}", valueType, parameterType);


                }
        } else if(parameterType.equals(PARAMETER_TYPE_CATEGORY)){
            try {
                switch (valueType) {
                    case VALUE_TYPE_INT8:
                        if ((Integer) value > Byte.MAX_VALUE)
                            log.error("BUFFER OVERFLOW: value {} found in {} parameter {}", value, valueType, paramName);
                        break;
                    default:
                        log.error("UNEXPECTED VALUE TYPE {} for parameter type {}", valueType, parameterType);
                }
            } catch(NumberFormatException e){
                log.error("Expected VALUE TYPE {}, received VALUE {} for parameter {}", valueType, value, paramName);
            }
        } else if(parameterType.equals(PARAMETER_TYPE_BOOLEAN)){

                switch(valueType){
                    case VALUE_TYPE_INT8:
                        if(!(value instanceof Boolean))
                            log.error("Expected VALUE TYPE {}, received VALUE {} for parameter {}",
                                    valueType, value, paramName);
                        break;
                    default:
                        log.error("UNEXPECTED VALUE TYPE {} for parameter type {}", valueType, parameterType);
                }
        } else{
            log.error("UNEXPECTED PARAMETER TYPE {}", parameterType);
        }
    }



    public boolean doesParameterExistInSample(Map sample, String paramName){
        if (!sample.containsKey(paramName)) {
            log.error("MISSING PARAMETER FROM STREAM: {}", paramName);
            return false;
        }
        return true;
    }

    public void findExtraParams(Map<String, Object> sample, DataStream ds){
        for(String paramName: sample.keySet()){
            if(!ds.getParams().containsKey(paramName)){
                log.error("UNDOCUMENTED PARAMETER FROM STREAM: {}", paramName);
            }
        }
    }
}