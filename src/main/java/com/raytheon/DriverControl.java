package com.raytheon;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DriverControl {
    private final ZMQ.Socket command_socket;
    private static Logger log = LogManager.getLogger();
    private DriverModel model;
    private volatile boolean isCommanding = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public DriverControl(String host, int port, DriverModel model) {
        this.model = model;
        String url = "tcp://" + host + ":" + port;
        log.debug("Initialize DriverControl");
        ZContext context = new ZContext();
        command_socket = context.createSocket(ZMQ.REQ);
        command_socket.connect(url);
        log.debug("Command socket connected! ({})", url);
    }

    protected Future<String> ping() {
        return sendCommand(DriverCommandEnum.PING, 30, "ping from java");
    }

    protected Future<String> configure() {
        return sendCommand(DriverCommandEnum.CONFIGURE, model.getConfig().getPortAgentConfig());
    }

    protected Future<String> init() {
        return sendCommand(DriverCommandEnum.SET_INIT_PARAMS, model.getConfig().getStartupConfig());
    }

    protected Future<String> connect() {
        return sendCommand(DriverCommandEnum.CONNECT);
    }

    protected Future<String> discover() {
        return sendCommand(DriverCommandEnum.DISCOVER_STATE);
    }

    protected Future<String> stop() {
        return sendCommand(DriverCommandEnum.STOP_DRIVER);
    }

    protected Future<String> getMetadata() {
        return sendCommand(DriverCommandEnum.GET_CONFIG_METADATA);
    }

    protected Future<String> getCapabilities() {
        return sendCommand(DriverCommandEnum.GET_CAPABILITIES);
    }

    protected Future<String> execute(String command) {
        return sendCommand(DriverCommandEnum.EXECUTE_RESOURCE, command);
    }

    protected Future<String> getProtocolState() {
        return sendCommand(DriverCommandEnum.GET_RESOURCE_STATE, 5);
    }

    protected Future<String> getResource(String... resources) {
        return sendCommand(DriverCommandEnum.GET_RESOURCE, resources);
    }

    protected Future<String> setResource(String parameters) {
        return sendCommand(DriverCommandEnum.SET_RESOURCE, parameters);
    }

    private String _sendCommand(DriverCommandEnum command, long timeout, String... args) {
        Platform.runLater(() -> model.setStatus(String.format("sending command %s...", command.toString())));
        command_socket.send(buildCommand(command, args).toString());
        String reply = null;

        // loop on the command socket for a response
        Instant endTime = Instant.now().plusSeconds(timeout);
        while (Instant.now().isBefore(endTime)) {
            ZMsg msg = ZMsg.recvMsg(command_socket, ZMQ.NOBLOCK);
            if (msg != null) {
                reply = msg.popString();
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        log.debug("receive loop complete, reply: {}", reply);

        if (reply != null) {
            log.debug("received reply: {}", reply);
            if (reply.startsWith("[")) {
                JSONArray possibleException = new JSONArray(reply);
                if (possibleException.length() == 3) {
                    log.error("EXCEPTION FROM DRIVER: {}", possibleException);
                    return reply;
                }
            }
            if (reply.startsWith("\"")) {
                reply = reply.substring(1, reply.length() - 1);
            }
            log.debug("about to switch: {}", command);
            switch (command) {
                case GET_CONFIG_METADATA:
                    final JSONObject metaData = new JSONObject(reply.replace("\\\"", "\""));
                    Platform.runLater(() -> model.parseMetadata(metaData));
                    break;
                case GET_CAPABILITIES:
                    final JSONArray capes = new JSONArray(reply).getJSONArray(0);
                    Platform.runLater(() -> model.parseCapabilities(capes));
                    break;
                case GET_RESOURCE_STATE:
                    final String state = reply;
                    Platform.runLater(() -> model.setState(state));
                    break;
                case GET_RESOURCE:
                    final JSONObject resource = new JSONObject(reply);
                    Platform.runLater(() -> model.setParams(resource));
                    break;
                default:
                    log.debug("hit default in switch statement");
                    break;
            }
        } else {
            log.debug("no reply received!");
        }
        log.debug(reply);
        isCommanding = false;

        return reply;
    }

    private Future<String> sendCommand(DriverCommandEnum command, String... args) {
        return sendCommand(command, 600, args);
    }

    private Future<String> sendCommand(DriverCommandEnum command, long timeout, String... args) {
        log.debug("sending command to driver {}", command);
        if (isCommanding) {
            Platform.runLater(() -> model.setStatus(String.format(
                    "unable to send command %s while processing previous command",
                    command.toString())));
            return executor.submit(() -> "busy processing previous command");
        }

        isCommanding = true;
        return executor.submit(() -> this._sendCommand(command, timeout, args));
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
}
