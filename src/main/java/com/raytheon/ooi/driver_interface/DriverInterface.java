package com.raytheon.ooi.driver_interface;

import com.raytheon.ooi.common.Constants;
import com.raytheon.ooi.common.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.raytheon.ooi.common.JsonHelper.toJson;

/**
 * Abstract class representing a generic interface to an Instrument Driver
 */

public abstract class DriverInterface extends Observable {
    private static final Logger log = LoggerFactory.getLogger(DriverInterface.class);
    private static final int DEFAULT_TIMEOUT = 600;
    protected boolean connected = false;

    public String ping() {
        return (String) sendCommand(Constants.PING, 5, "ping from java");
    }

    public void configurePortAgent(Map portAgentConfig) {
        sendCommand(Constants.CONFIGURE, 15, portAgentConfig);
    }

    public void initParams(Map startupConfig) {
        sendCommand(Constants.SET_INIT_PARAMS, 5, startupConfig);
    }

    public void connect() {
        sendCommand(Constants.CONNECT, 15);
    }

    public void discoverState() {
        sendCommand(Constants.DISCOVER_STATE, DEFAULT_TIMEOUT);
    }

    public void stopDriver() {
        sendCommand(Constants.STOP_DRIVER, 5);
    }

    public Map getMetadata() {
        Object reply = sendCommand(Constants.GET_CONFIG_METADATA, 5);
        if (reply instanceof Map)
            return (Map) reply;
        return null;
    }

    public List getCapabilities() {
        Object reply = sendCommand(Constants.GET_CAPABILITIES, 5);
        if (reply instanceof List)
            return (List) reply;
        return null;
    }

    public Object execute(String command) {
        log.debug("Execute received command: {}", command);
        return sendCommand(Constants.EXECUTE_RESOURCE, DEFAULT_TIMEOUT, command);
    }

    public String getProtocolState() {
        return (String) sendCommand(Constants.GET_RESOURCE_STATE, 5);
    }

    public Map getResource(String... resources) {
        Object reply = sendCommand(Constants.GET_RESOURCE, DEFAULT_TIMEOUT, (Object)resources);
        if (reply instanceof Map)
            return (Map) reply;
        return null;
    }

    public void setResource(Map parameters) {
        sendCommand(Constants.SET_RESOURCE, DEFAULT_TIMEOUT, parameters);
    }

    public boolean isConnected() {
        return connected;
    }

    private Object sendCommand(String c, int timeout, Object... args) {
        try {
            String command = buildCommand(c, args);
            log.debug("Sending command: {}", command);
            String reply = _sendCommand(command, timeout);
            log.debug("Received reply: {}", reply);
            if (reply == null)
                return null;
            Object obj = null;
            try {
                obj = JsonHelper.toObject(reply);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (obj instanceof List)
                if (((List) obj).size() == 3) {
                    handleException((List) obj);
                    return null;
                }
            return obj;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private String buildCommand(String command, Object... args) throws IOException {
        Map<String, Object> message = new HashMap<>();
        Map<String, Object> keyword_args = new HashMap<>();
        List<Object> message_args = new ArrayList<>();
        Arrays.asList(args).forEach(message_args::add);

        message.put("cmd", command);
        message.put("args", message_args);
        message.put("kwargs", keyword_args);
        log.debug("BUILT COMMAND: {}", message);
        return toJson(message);
    }

    protected abstract String _sendCommand(String command, int timeout);

    protected abstract void eventLoop();

    public abstract void shutdown();

    public void handleException(List exception) {
        // TODO
        log.error("handleException: {}", exception);
    }
}
