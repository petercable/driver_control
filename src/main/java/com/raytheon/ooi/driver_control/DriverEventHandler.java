package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.common.JsonHelper;
import com.raytheon.ooi.preload.DataStream;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


public class DriverEventHandler implements Observer {

    private final DriverModel model = DriverModel.getInstance();
    private final static Logger log = LoggerFactory.getLogger(DriverEventHandler.class);

    public DriverEventHandler() {}

    @Override
    public void update(Observable o, Object arg) {
        log.debug("EVENTOBSERVER GOT: {} {}", o, arg);
        try {
            Map event = JsonHelper.toMap((String) arg);
            switch ((String)event.get("type")) {
                case Constants.STATE_CHANGE_EVENT:
                    Platform.runLater(()-> {
                        model.setState((String)event.get("value"));
                    });
                    break;
                case Constants.SAMPLE_EVENT:
                    try {
                        final DataStream sample = DriverSampleFactory.parseSample((String) event.get("value"));
                        log.info("Received SAMPLE event: " + sample);
                        Platform.runLater(()->model.publishSample(sample));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case Constants.CONFIG_CHANGE_EVENT:
                    Platform.runLater(()->model.setParams((Map)event.get("value")));
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
