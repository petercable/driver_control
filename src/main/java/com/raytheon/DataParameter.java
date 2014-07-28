package com.raytheon;

public class DataParameter {
    private String name;
    private String parameterType;
    private String valueEncoding;
    private String parameterFunctionId;
    private String parameterFunctionMap;

    public DataParameter(String name, String parameterType, String valueEncoding,
                         String parameterFunctionId, String parameterFunctionMap) {
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

    public String toString() {
        return String.format("NAME: %s TYPE: %s ENCODING: %s FUNCID: %s FUNCMAP: %s",
                name,
                parameterType,
                valueEncoding,
                parameterFunctionId,
                parameterFunctionMap);
    }
}
