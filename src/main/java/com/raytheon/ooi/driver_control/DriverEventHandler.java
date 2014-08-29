package com.raytheon.ooi.driver_control;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.common.JsonHelper;
import com.raytheon.ooi.preload.DataStream;
import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;

import static com.raytheon.ooi.common.JsonHelper.toJson;


public class DriverEventHandler implements Observer {

    private final DriverModel model = DriverModel.getInstance();
    private final static Logger log = LoggerFactory.getLogger(DriverEventHandler.class);
    private MessageProducer messageProducer;
    private Session session;

    public DriverEventHandler() { }

    private void createProducer() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/jmsPublisher.properties"));
        Context context = new InitialContext(properties);
        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
        Connection connection = connectionFactory.createConnection();
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) context.lookup("topicExchange");

        messageProducer = session.createProducer(destination);
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
                        final DataStream sample = DriverSampleFactory.parseSample((String) event.get("value"));
                        log.info("Received SAMPLE event: " + sample);
                        Platform.runLater(()->model.publishSample(sample));
                        if (!sample.getName().equals("raw")) {
                            publishJMS((String)arg);
                        }
                    } catch (Exception e) {
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

    public void publishJMS(String event) throws Exception {
        if (messageProducer == null)
            createProducer();
        log.info("Publish to JMS: {}", event);
        messageProducer.send(session.createTextMessage(event));
    }
}
