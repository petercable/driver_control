package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.common.JsonHelper;
import com.raytheon.ooi.preload.DataStream;
import com.raytheon.ooi.preload.PreloadDatabase;
import com.raytheon.ooi.preload.SqlitePreloadDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverSampleFactory {
    private static final PreloadDatabase db = SqlitePreloadDatabase.getInstance();
    private static final Logger log = LogManager.getLogger(DriverSampleFactory.class);

    private DriverSampleFactory() {
    }

    public static DataStream parseSample(String s) throws IOException {
        Map<String, Object> map = new HashMap<>();
        Map json = JsonHelper.toMap(s);

        map.put(Constants.STREAM_NAME, json.get(Constants.STREAM_NAME));
        map.put(Constants.PREFERRED_TIMESTAMP, json.get(Constants.PREFERRED_TIMESTAMP));
        map.put(Constants.QUALITY_FLAG, json.get(Constants.QUALITY_FLAG));
        map.put(Constants.PORT_TIMESTAMP, json.get(Constants.PORT_TIMESTAMP));
        map.put(Constants.DRIVER_TIMESTAMP, json.get(Constants.DRIVER_TIMESTAMP));
        map.put(Constants.PKT_FORMAT_ID, json.get(Constants.PKT_FORMAT_ID));
        map.put(Constants.PKT_VERSION, json.get(Constants.PKT_VERSION));

        log.trace("Loading instrument supplied values into sample object...");
        for (Object json_value : (List)json.get(Constants.VALUES)) {
            Map element = (Map) json_value;
            log.debug(element);
            Object value = element.get(Constants.VALUE);
            if (value == null) value = "";
            if (element.containsKey("binary")) {
                value = Base64.getDecoder().decode((String)value);
            }
            map.put((String) element.get(Constants.VALUE_ID), value);
        }

        // retrieve the stream definition from preload
        DataStream stream = db.getStream((String) json.get(Constants.STREAM_NAME));
        stream.setValues(map);

        // validate the stream and archive the results
        stream.validate();
        stream.archive();

        // return the processed data for display
        return stream;
    }
}
