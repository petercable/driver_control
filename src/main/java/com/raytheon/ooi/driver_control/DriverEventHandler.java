package com.raytheon.ooi.driver_control;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Observable;
import java.util.Observer;


public class DriverEventHandler implements Observer {

    private final DriverModel model;
    private final static Logger log = LogManager.getLogger(DriverEventHandler.class);
    private final static String STATE_CHANGE_EVENT = "DRIVER_EVENT_STATE_CHANGE";
    private final static String SAMPLE_EVENT = "DRIVER_ASYNC_EVENT_SAMPLE";

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
            String eventValue = (String) jsonEvent.get("value");
            Double eventTime = (Double) jsonEvent.get("time");
            log.debug("type: {}, value: {}, time: {}", eventType, eventValue, eventTime);
            switch (eventType) {
                case STATE_CHANGE_EVENT:
                    Platform.runLater(()->model.setState(eventValue));
                    break;
                case SAMPLE_EVENT:
                    //Platform.runLater(()->model.publishSample());
                    break;
            }
        }
    }
}
