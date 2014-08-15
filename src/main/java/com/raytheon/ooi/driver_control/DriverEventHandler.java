package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.preload.PreloadDatabase;
import com.raytheon.ooi.preload.SqlitePreloadDatabase;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;


public class DriverEventHandler implements Observer {

    private final DriverModel model;
    private final PreloadDatabase db = SqlitePreloadDatabase.getInstance();
    private DriverConfig config;
    private final static Logger log = LogManager.getLogger(DriverEventHandler.class);
    private final static String STATE_CHANGE_EVENT = "DRIVER_ASYNC_EVENT_STATE_CHANGE";
    private final static String SAMPLE_EVENT = "DRIVER_ASYNC_EVENT_SAMPLE";
    private final static String CONFIG_CHANGE_EVENT = "DRIVER_ASYNC_EVENT_CONFIG_CHANGE";

    public DriverEventHandler(DriverModel model) {
        this.model = model;
    }

    @Override
    public void update(Observable o, Object arg) {
        log.debug("EVENTOBSERVER GOT: {} {}", o, arg);
        Object event = JSONValue.parse(arg.toString());
        if (event instanceof JSONObject) {
            JSONObject jsonEvent = (JSONObject) event;
            String eventType = (String) jsonEvent.get("type");
            Object eventValue = jsonEvent.get("value");
            Double eventTime = (Double) jsonEvent.get("time");
            log.debug("type: {}, value: {}, time: {}", eventType, eventValue, eventTime);
            switch (eventType) {
                case STATE_CHANGE_EVENT:
                    Platform.runLater(()->model.setState((String)eventValue));
                    break;
                case SAMPLE_EVENT:
                    Map<String, Object> sample = DriverSampleFactory.parseSample((String)eventValue, config);
                    log.info("Received SAMPLE event: " + sample);
                    Platform.runLater(()->model.publishSample(sample));
                    break;
                case CONFIG_CHANGE_EVENT:
                    Platform.runLater(()->model.setParams((JSONObject) eventValue));
                    break;
            }
        }
    }

    public void setConfig(DriverConfig config) {
        this.config = config;
    }
}
