package com.raytheon.ooi.driver_control;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class DriverSampleFactory {
    public static final String STREAM_NAME = "stream_name";  // s
    public static final String QUALITY_FLAG = "quality_flag";  //s
    public static final String PREFERRED_TIMESTAMP = "preferred_timestamp";  //s
    public static final String PORT_TIMESTAMP = "port_timestamp";  //d
    public static final String DRIVER_TIMESTAMP = "driver_timestamp";  //d
    public static final String PKT_FORMAT_ID = "pkt_format_id";  //s
    public static final String PKT_VERSION = "pkt_version";  //i
    public static final String VALUE = "value";
    public static final String VALUES = "values";
    public static final String VALUE_ID = "value_id";

    private DriverSampleFactory() {}

    public static Map<String, Object> parseSample(String s) {
        Map<String, Object> map = new HashMap<String, Object>();
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
        return map;
    }
}
