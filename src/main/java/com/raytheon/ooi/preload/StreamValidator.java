package com.raytheon.ooi.preload;

public class StreamValidator {
//    public void validateStreams() {
//
//        Map<String, DataStream> map = preload.getStreams(model.getConfig().getScenario());
//        map.entrySet().forEach(System.out::println);
//        map.keySet().stream().filter(model.sampleLists::containsKey).forEach(key -> {
//            ObservableList<Map<String, Object>> samples = model.sampleLists.get(key);
//            Map sample = samples.get(0);
//            DataStream ds = map.get(key);
//            log.debug("going to compare {} to {}", sample, ds);
//            for (String paramName : ds.getParams().keySet()) {
//                DataParameter parameter = ds.getParams().get(paramName);
//                if (!sample.containsKey(paramName))
//                    log.error("MISSING PARAMETER FROM STREAM: {}", paramName);
//                else {
//                    Object value = sample.get(paramName);
//                    log.debug("Testing {} value: {}", paramName, value);
//                    if (parameter.getParameterType().equals("quantity")) {
//                        switch (parameter.getValueEncoding()) {
//                            case "int32":
//                                if (!(value instanceof Integer)) {
//                                    log.error("Non integral value found in Integer field");
//                                    break;
//                                }
//                                if ((Integer) value > Integer.MAX_VALUE)
//                                    log.error("Oversized (>int32) integral value found in Integer field");
//                            case "int16":
//                                if ((Integer) value > Short.MAX_VALUE)
//                                    log.error("Oversized (>int16) integral value found in Integer field");
//                            case "int8":
//                                if ((Integer) value > Byte.MAX_VALUE)
//                                    log.error("Oversized (>int8) integral value found in Integer field");
//                                break;
//                            case "float64":
//                                if (!(value instanceof Double)) {
//                                    if (value instanceof Integer) {
//                                        value = ((Integer) value).doubleValue();
//                                    } else {
//                                        log.error("Non floating point value found in FP field");
//                                        break;
//                                    }
//                                }
//                                if ((Double) value > Double.MAX_VALUE)
//                                    log.error("Oversized FP (double) value found in FP field");
//                            case "float32":
//                                if ((Double) value > Float.MAX_VALUE)
//                                    log.error("Oversized FP (float) value found in FP field");
//                                break;
//                            case "str":
//                                break;
//                            default:
//                                log.error("UNHANDLED VALUE ENCODING {} {}", paramName, parameter.getValueEncoding());
//                                break;
//                        }
//                    } else {
//                        log.debug("Non-quantity field [{}] not checked (type={})", paramName, parameter.getParameterType());
//                    }
//                }
//            }
//        });
//    }
}
