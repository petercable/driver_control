package com.raytheon.ooi.driver_control;

import java.util.HashMap;
import java.util.Map;

public class DataStream {
    private String name;
    private Map<String, DataParameter> params = new HashMap<>();

    public DataStream(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, DataParameter> getParams() {
        return params;
    }

    public String toString() {
        return String.format("DataStream(%s)", name);
    }
}
