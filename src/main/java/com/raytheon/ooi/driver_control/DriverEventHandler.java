package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.common.Constants;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;


public class DriverEventHandler implements Observer {

    private final DriverModel model = DriverModel.getInstance();
    private final static Logger log = LogManager.getLogger(DriverEventHandler.class);

    public DriverEventHandler() {}

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
                case Constants.STATE_CHANGE_EVENT:
                    Platform.runLater(()->model.setState((String)eventValue));
                    break;
                case Constants.SAMPLE_EVENT:
                    Map<String, Object> sample = DriverSampleFactory.parseSample((String)eventValue);
                    log.info("Received SAMPLE event: " + sample);
                    Platform.runLater(()->model.publishSample(sample));
                    break;
                case Constants.CONFIG_CHANGE_EVENT:
                    Platform.runLater(()->model.setParams((JSONObject) eventValue));
                    break;
            }
        }
    }
}
