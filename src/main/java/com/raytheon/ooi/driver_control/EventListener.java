package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.preload.PreloadDatabase;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Map;

public class EventListener extends Thread {

    private ZContext context;
    private ZMQ.Socket event_socket;
    private static Logger log = LogManager.getLogger("EventListener");
    private boolean keepRunning = true;
    private DriverModel model;
    private DriverControl controller;
    private PreloadDatabase db;
    private static final String VALUE = "value";

    public EventListener(String host, int port, DriverModel model, DriverControl controller, PreloadDatabase db) {
        this.model = model;
        this.controller = controller;
        this.db = db;
        log.debug("Initialize EventListener");
        context = new ZContext();
        event_socket = context.createSocket(ZMQ.SUB);
        // connect to the event port
        event_socket.connect(String.format("tcp://%s:%d", host, port));
        event_socket.subscribe(new byte[0]);
        log.debug("Event socket connected!");
        this.setName("event listener thread");
    }

    public void run() {
        while (keepRunning) {
            ZMsg msg = ZMsg.recvMsg(event_socket, ZMQ.NOBLOCK);
            if (msg == null) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) { }
                continue;
            }
            for (Object aMsg : msg) {
                String data = aMsg.toString();
                if (data == null) break;
                JSONObject event = new JSONObject(data);
                double time = event.getDouble("time");
                String type = event.getString("type");

                if (MessageTypes.SAMPLE.equals(type)) {
                    Map<String, Object> sample = DriverSampleFactory.parseSample(event.getString(VALUE), db);
                    log.info("Received SAMPLE event: " + sample);
                    model.publishSample(sample);
                } else if (MessageTypes.CONFIG_CHANGE.equals(type)) {
                    log.info("Received CONFIG_CHANGE event: " + event);
                    model.setParams(event.getJSONObject(VALUE));
                } else if (MessageTypes.STATE_CHANGE.equals(type)) {
                    log.info("Received STATE CHANGE event: " + event);
                    controller.getCapabilities();
                    Platform.runLater(() -> model.setState(event.getString(VALUE)));
                } else {
                    log.info(event);
                }
            }
        }
        context.close();
    }

    public void shutdown() {
        keepRunning = false;
    }

    private JSONObject receive(ZMQ.Socket subscriber)
    {
        String data = subscriber.recvStr();
        return data == null ? null : new JSONObject(data);
    }
}
