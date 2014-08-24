package com.raytheon.ooi.common;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonHelper {
    private final static ObjectMapper mapper = new ObjectMapper();
    private JsonHelper() {}

    public static String toJson(Object obj) throws IOException {
        return mapper.writeValueAsString(obj);
    }

    public static Map toMap(String json) throws IOException {
        return mapper.readValue(json, Map.class);
    }

    public static List toList(String json) throws IOException {
        return mapper.readValue(json, List.class);
    }

    public static Object toObject(String json) throws IOException {
        JsonNode node = mapper.readTree(json);
        if (node.isArray()) {
            return toList(json);
        } else if (node.isObject()) {
            return toMap(json);
        } else {
            return node.getTextValue();
        }
    }
}
