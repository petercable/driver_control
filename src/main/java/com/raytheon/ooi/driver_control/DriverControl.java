package com.raytheon.ooi.driver_control;

import javafx.application.Platform;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.*;

public class DriverControl {
    private final ZMQ.Socket command_socket;
    private static Logger log = LogManager.getLogger("DriverControl");
    private DriverModel model;
    private volatile boolean isCommanding = false;
    private ExecutorService executor;
    private ScheduledExecutorService scheduler;
    private final String busy = "busy processing previous command";

    public DriverControl(String host, int port, DriverModel model) {
        this.model = model;
        String url = String.format("tcp://%s:%d", host, port);
        log.debug("Initialize DriverControl");
        ZContext context = new ZContext();
        command_socket = context.createSocket(ZMQ.REQ);
        command_socket.connect(url);
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("controllerPool-%d")
                .daemon(true)
                .priority(Thread.NORM_PRIORITY)
                .build();
        BasicThreadFactory scheduleFactory = new BasicThreadFactory.Builder()
                .namingPattern("controllerScheduler")
                .daemon(true)
                .priority(Thread.NORM_PRIORITY)
                .build();
        executor = Executors.newCachedThreadPool(factory);
        getMetadata();
        scheduler = Executors.newSingleThreadScheduledExecutor(scheduleFactory);
        scheduler.scheduleAtFixedRate(this::ping, 0, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::getCapabilities, 3, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::getProtocolState, 6, 10, TimeUnit.SECONDS);
        Platform.runLater(()->model.setConnection("CONNECTED"));
        log.debug("Command socket connected! ({})", url);
    }

    protected Future<String> ping() {
        return sendCommand(DriverCommandEnum.PING, 5, "ping from java");
    }

    protected Future<String> configure() {
        return sendCommand(DriverCommandEnum.CONFIGURE, 15, model.getConfig().getPortAgentConfig());
    }

    protected Future<String> init() {
        return sendCommand(DriverCommandEnum.SET_INIT_PARAMS, 5, model.getConfig().getStartupConfig());
    }

    protected Future<String> connect() {
        return sendCommand(DriverCommandEnum.CONNECT, 15);
    }

    protected Future<String> discover() {
        return sendCommand(DriverCommandEnum.DISCOVER_STATE);
    }

    protected Future<String> stop() {
        return sendCommand(DriverCommandEnum.STOP_DRIVER, 5);
    }

    protected Future<String> getMetadata() {
        return sendCommand(DriverCommandEnum.GET_CONFIG_METADATA, 5);
    }

    protected Future<String> getCapabilities() {
        return sendCommand(DriverCommandEnum.GET_CAPABILITIES, 5);
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

    private String _sendCommand(DriverCommandEnum command, int timeout, String... args) {
        Platform.runLater(() -> model.setStatus(String.format("sending command %s...", command.toString())));
        String reply = null;

        try {
            command_socket.send(buildCommand(command, args).toString());
            command_socket.setReceiveTimeOut(timeout * 1000);
            reply = command_socket.recvStr();
        } finally {
            isCommanding = false;
            Platform.runLater(()->model.setConnection("IDLE"));
        }

        log.debug("receive loop complete, reply: {}", reply);

        if (reply == null) {
            // No response from instrument, set to disconnected
            Platform.runLater(()-> {
                model.setStatus("NO REPLY FROM INSTRUMENT");
                model.setConnection("DISCONNECTED");
            });
        } else {
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
            switch (command) {
                case GET_CONFIG_METADATA:
                    final JSONObject metaData = new JSONObject(reply.replace("\\\"", "\""));
                    Platform.runLater(() -> {
                        model.parseMetadata(metaData);
                        model.setStatus("received meta data");
                    });
                    break;
                case GET_CAPABILITIES:
                    final JSONArray capes = new JSONArray(reply).getJSONArray(0);
                    Platform.runLater(() -> {
                        model.parseCapabilities(capes);
                        model.setStatus("received capabilities");
                    });
                    break;
                case GET_RESOURCE_STATE:
                    final String state = reply;
                    Platform.runLater(() -> {
                        model.setState(state);
                        model.setStatus("received state");
                    });
                    break;
                case GET_RESOURCE:
                    final JSONObject resource = new JSONObject(reply);
                    Platform.runLater(() -> {
                        model.setParams(resource);
                        model.setStatus("received resource");
                    });
                    break;
                default:
                    log.trace("hit default in switch statement");
                    Platform.runLater(() -> model.setStatus(""));
                    break;
            }
        }
        return reply;
    }

    private Future<String> sendCommand(DriverCommandEnum command, String... args) {
        return sendCommand(command, 600, args);
    }

    private Future<String> sendCommand(DriverCommandEnum command, int timeout, String... args) {
        log.debug("sending command to driver {}", command);
        if (isCommanding) {
            String status = String.format(
                    "unable to send command %s while processing previous command",
                    command.toString());
            log.debug(status);
            Platform.runLater(() -> model.setStatus(status));
            return executor.submit(() -> busy);
        }

        isCommanding = true;
        Platform.runLater(()->model.setConnection("BUSY"));
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
