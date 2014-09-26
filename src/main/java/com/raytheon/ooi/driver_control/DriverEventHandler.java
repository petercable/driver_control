package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.common.JsonHelper;
import com.raytheon.ooi.preload.DataStream;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


public class DriverEventHandler implements Observer {

    private final DriverModel model = DriverModel.getInstance();
    private final static Logger log = LoggerFactory.getLogger(DriverEventHandler.class);
    private FileWriter edexWriter;

    public DriverEventHandler(String sensor) {
        try {
            edexWriter = new FileWriter("edex_" + sensor + ".json", false);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        log.debug("EVENTOBSERVER GOT: {} {}", o, arg);
        try {
            Map event = JsonHelper.toMap((String) arg);
            switch ((String)event.get("type")) {
                case Constants.STATE_CHANGE_EVENT:
                    Platform.runLater(()-> model.setState((String)event.get("value")));
                    break;
                case Constants.SAMPLE_EVENT:
                    try {
                        String particleString = (String)event.get("value");
                        Map<String, Object> particle = JsonHelper.toMap(particleString);
                        log.info("Dumping json looking like this: " + particleString);
                        if (! particle.get("stream_name").equals("raw")) {
                            log.info("dumping json to file");
                            edexWriter.write(particleString + "\n\n");
                            edexWriter.flush();
                        }

                        final DataStream sample = DriverSampleFactory.parseSample(particleString);
                        log.info("Received SAMPLE event: " + sample);
                        Platform.runLater(()->model.publishSample(sample));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case Constants.CONFIG_CHANGE_EVENT:
                    Platform.runLater(()->model.setParams((Map) event.get("value")));
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
