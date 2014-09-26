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
    private final static String edexYaml = "/tmp/edex.yaml";
    private FileWriter edexWriter;

    public DriverEventHandler() {
        try {
            edexWriter = new FileWriter(edexYaml, false);
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
                        String eventString = (String)event.get("value");
                        Map myParticle = JsonHelper.toMap(eventString);
                        Yaml yaml = new Yaml();

                        String thing = yaml.dump(myParticle);
                        log.info("Dumping yaml looking like this:...\n" + thing);
                        if (! myParticle.get("stream_name").equals("raw")) {
                            log.info("dumping yaml to file");
                            yaml.dump(myParticle, edexWriter);
                            edexWriter.write("\n");
                            edexWriter.flush();
                        }

                        final DataStream sample = DriverSampleFactory.parseSample(eventString);
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
