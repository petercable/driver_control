package com.raytheon.ooi.driver_interface;

import com.raytheon.ooi.driver_control.DriverCommandEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONValue;

import java.util.Observable;

/**
 * Abstract class representing a generic interface to an Instrument Driver
 */

public abstract class DriverInterface extends Observable {
    private static Logger log = LogManager.getLogger(DriverInterface.class);

    public String ping() {
        return (String) sendCommand(DriverCommandEnum.PING, 5, "ping from java");
    }

    public Object configurePortAgent(String portAgentConfig) {
        return sendCommand(DriverCommandEnum.CONFIGURE, 15, portAgentConfig);
    }

    public Object initParams(String startupConfig) {
        return sendCommand(DriverCommandEnum.SET_INIT_PARAMS, 5, startupConfig);
    }

    public Object connect() {
        return sendCommand(DriverCommandEnum.CONNECT, 15);
    }

    public Object discover() {
        return sendCommand(DriverCommandEnum.DISCOVER_STATE);
    }

    public Object stop() {
        return sendCommand(DriverCommandEnum.STOP_DRIVER, 5);
    }

    public Object getMetadata() {
        return sendCommand(DriverCommandEnum.GET_CONFIG_METADATA, 5);
    }

    public Object getCapabilities() {
        return sendCommand(DriverCommandEnum.GET_CAPABILITIES, 5);
    }

    public Object execute(String command) {
        return sendCommand(DriverCommandEnum.EXECUTE_RESOURCE, command);
    }

    public String getProtocolState() {
        return (String) sendCommand(DriverCommandEnum.GET_RESOURCE_STATE, 5);
    }

    public Object getResource(String... resources) {
        return sendCommand(DriverCommandEnum.GET_RESOURCE, resources);
    }

    public Object setResource(String parameters) {
        return sendCommand(DriverCommandEnum.SET_RESOURCE, parameters);
    }

    private Object sendCommand(DriverCommandEnum c, int timeout, String... args) {
        String command = buildCommand(c, args).toString();
        log.debug("Sending command: {}", command);
        String reply = _sendCommand(command, timeout);
        log.debug("Received reply: {}", reply);
        Object obj = JSONValue.parse(reply);
        log.debug("Parsed reply: {}", obj);
        return obj;
    }

    private Object sendCommand(DriverCommandEnum c, String... args) {
        return sendCommand(c, 600, args);
    }

    private JSONObject buildCommand(DriverCommandEnum command, String... args) {
        JSONObject message = new JSONObject();
        JSONObject keyword_args = new JSONObject();
        JSONArray message_args = new JSONArray();

        for (String arg : args) {
            try {
                message_args.put(new JSONObject(arg));
            } catch (JSONException e) {
                message_args.put(arg);
            }
        }
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
