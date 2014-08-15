package com.raytheon.ooi.driver_interface;

import com.raytheon.ooi.common.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Arrays;
import java.util.Observable;

/**
 * Abstract class representing a generic interface to an Instrument Driver
 */

public abstract class DriverInterface extends Observable {
    private static Logger log = LogManager.getLogger(DriverInterface.class);
    protected boolean connected = false;

    public String ping() {
        return (String) sendCommand(Constants.PING, 5, "ping from java");
    }

    public void configurePortAgent(String portAgentConfig) {
        sendCommand(Constants.CONFIGURE, 15, portAgentConfig);
    }

    public void initParams(String startupConfig) {
        sendCommand(Constants.SET_INIT_PARAMS, 5, startupConfig);
    }

    public void connect() {
        sendCommand(Constants.CONNECT, 15);
    }

    public void discoverState() {
        sendCommand(Constants.DISCOVER_STATE);
    }

    public void stopDriver() {
        sendCommand(Constants.STOP_DRIVER, 5);
    }

    public JSONObject getMetadata() {
        Object reply = sendCommand(Constants.GET_CONFIG_METADATA, 5);
        if (reply instanceof String)
            return (JSONObject) JSONValue.parse((String)reply);
        if (reply instanceof JSONObject)
            return (JSONObject) reply;
        return null;
    }

    public JSONArray getCapabilities() {
        Object reply = sendCommand(Constants.GET_CAPABILITIES, 5);
        if (reply instanceof org.json.simple.JSONArray)
            return (org.json.simple.JSONArray) reply;
        return null;
    }

    public Object execute(String command) {
        log.debug("Execute received command: {}", command);
        return sendCommand(Constants.EXECUTE_RESOURCE, command);
    }

    public String getProtocolState() {
        return (String) sendCommand(Constants.GET_RESOURCE_STATE, 5);
    }

    public Object getResource(String... resources) {
        Object reply = sendCommand(Constants.GET_RESOURCE, resources);
        log.debug("getResource reply = {}", reply);
        return reply;
    }

    public Object setResource(String parameters) {
        return sendCommand(Constants.SET_RESOURCE, parameters);
    }

    public boolean isConnected() {
        return connected;
    }

    private Object sendCommand(String c, int timeout, String... args) {
        String command = buildCommand(c, args).toString();
        log.debug("Sending command: {}", command);
        String reply = _sendCommand(command, timeout);
        log.debug("Received reply: {}", reply);
        Object obj = null;
        if (reply == null)
            return null;
        obj = JSONValue.parse(reply);
        log.debug("Parsed reply: {}", obj);
        return obj;
    }

    private Object sendCommand(String c, String... args) {
        return sendCommand(c, 600, args);
    }

    @SuppressWarnings("unchecked")
    private org.json.simple.JSONObject buildCommand(String command, String... args) {
        org.json.simple.JSONObject message = new org.json.simple.JSONObject();
        org.json.simple.JSONObject keyword_args = new org.json.simple.JSONObject();
        org.json.simple.JSONArray message_args = new org.json.simple.JSONArray();
        Arrays.asList(args).forEach((s) -> {
            Object parsed = JSONValue.parse(s);
            log.debug("RAW: {} PARSED: {}", s, parsed);
            if (parsed == null) message_args.add(s);
            else message_args.add(parsed);
        });

        message.put("cmd", command);
        message.put("args", message_args);
        message.put("kwargs", keyword_args);
        log.debug("BUILT COMMAND: {}", message);
        return message;
    }

    protected abstract String _sendCommand(String command, int timeout);

    protected abstract void eventLoop();

    public abstract void shutdown();
}
