package com.raytheon.ooi.driver_interface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of the Instrument Driver interface for ZMQ
 */

public class ZmqDriverInterface extends DriverInterface {
    private final ZMQ.Socket commandSocket;
    private final ZMQ.Socket eventSocket;
    private boolean keepRunning = true;
    private List<Object> listeners = new ArrayList<>();
    private static Logger log = LogManager.getLogger(ZmqDriverInterface.class);

    public ZmqDriverInterface(String host, int commandPort, int eventPort) {
        String commandUrl = String.format("tcp://%s:%d", host, commandPort);
        String eventUrl = String.format("tcp://%s:%d", host, eventPort);
        
        log.debug("Initialize ZmqDriverInterface");
        ZContext context = new ZContext();
        
        log.debug("Connecting to command port: {}", commandUrl);
        commandSocket = context.createSocket(ZMQ.REQ);
        commandSocket.connect(commandUrl);

        log.debug("Connecting to event port: {}", eventUrl);
        eventSocket = context.createSocket(ZMQ.SUB);
        eventSocket.connect(eventUrl);
        eventSocket.subscribe(new byte[0]);
        
        log.debug("Connected, starting event loop");
        new Thread(this::eventLoop).start();
    }

    protected String _sendCommand(String command, int timeout) {
        commandSocket.send(command);
        commandSocket.setReceiveTimeOut(timeout * 1000);
        String reply = commandSocket.recvStr();
        if (reply == null)
            log.debug("Empty message received from command: {}", command);
        return reply;
    }

    protected void eventLoop() {
        while (keepRunning) {
            String reply = eventSocket.recvStr();
            if (reply != null)
                notifyObservers(reply);
            else
                log.debug("Empty message received in event loop");

        }
    }

    public void shutdown() {
        keepRunning = false;
        eventSocket.close();
        commandSocket.close();
    }
}
