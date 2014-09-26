package com.raytheon.ooi.driver_interface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;
import java.util.Observable;

/**
 * Concrete implementation of the Instrument Driver interface for ZMQ
 */

public class ZmqDriverEventInterface extends Observable {
    private final ZMQ.Socket eventSocket;
    private boolean keepRunning = true;
    private static Logger log = LoggerFactory.getLogger(ZmqDriverEventInterface.class);
    protected boolean connected = false;

    public ZmqDriverEventInterface(String host, int eventPort) {
        String eventUrl = String.format("tcp://%s:%d", host, eventPort);
        
        log.debug("Initialize ZmqDriverInterface");
        ZContext context = new ZContext();

        log.debug("Connecting to event port: {}", eventUrl);
        eventSocket = context.createSocket(ZMQ.SUB);
        eventSocket.connect(eventUrl);
        eventSocket.subscribe(new byte[0]);
        
        log.debug("Connected, starting event loop");
        Thread t = new Thread(this::eventLoop);
        t.setName("Event Loop");
        t.start();
        connected = true;
    }

    protected void eventLoop() {
        try {
            while (keepRunning) {
                String reply = eventSocket.recvStr();

                if (reply != null) {
                    log.debug("REPLY = {}, numObservers = {}", reply, countObservers());
                    try {
                        setChanged();
                        notifyObservers(reply);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        log.error("Exception notifying observers: {}", e.getMessage());
                    }
                } else {
                    log.debug("Empty message received in event loop");
                }
            }
        } catch (Exception e) {
            log.debug("Exception in eventLoop: {}", e.getMessage());
        }
    }

    public void shutdown() {
        keepRunning = false;
        eventSocket.close();
        connected = false;
    }

    public void handleException(List exception) {
        // TODO
        log.error("handleException: {}", exception);
    }
}
