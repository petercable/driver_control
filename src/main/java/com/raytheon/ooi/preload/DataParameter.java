package com.raytheon.ooi.preload;

public class DataParameter {
    private String id;
    private String name;
    private String parameterType;
    private String valueEncoding;
    private String parameterFunctionId;
    private String parameterFunctionMap;

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
}
