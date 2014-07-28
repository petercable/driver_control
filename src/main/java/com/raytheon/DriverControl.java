package com.raytheon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.time.Instant;

public class DriverControl {
    private final ZMQ.Socket command_socket;
    private static Logger log = LogManager.getLogger();
    private DriverModel model;

    public DriverControl(String host, int port, DriverModel model) {
        this.model = model;
        String url = "tcp://" + host + ":" + port;
        log.debug("Initialize DriverControl");
        ZContext context = new ZContext();
        command_socket = context.createSocket(ZMQ.REQ);
        command_socket.connect(url);
        log.debug("Command socket connected! ({})", url);
    }

    protected void ping() {
        sendCommand(buildCommand(DriverCommandEnum.PING, "ping from java"), 30);
    }

    protected void configure() {
        sendCommand(buildCommand(DriverCommandEnum.CONFIGURE, model.getConfig().getPortAgentConfig()));
    }

    protected void init() {
        sendCommand(buildCommand(DriverCommandEnum.SET_INIT_PARAMS, model.getConfig().getStartupConfig()));
    }

    protected void connect() {
        sendCommand(buildCommand(DriverCommandEnum.CONNECT));
    }

    protected void discover() {
        sendCommand(buildCommand(DriverCommandEnum.DISCOVER_STATE));
    }

    protected void stop() {
        sendCommand(buildCommand(DriverCommandEnum.STOP_DRIVER));
    }

    protected void getMetadata() {
        String reply = sendCommand(buildCommand(DriverCommandEnum.GET_CONFIG_METADATA));
        reply = reply.replace("\\\"", "\"");
        model.parseMetadata(new JSONObject(reply));
    }

    protected void getCapabilities() {
        String reply = sendCommand(buildCommand(DriverCommandEnum.GET_CAPABILITIES));
        JSONArray capes = new JSONArray(reply).getJSONArray(0);
        model.parseCapabilities(capes);
    }

    protected void execute(String command) {
        sendCommand(buildCommand(DriverCommandEnum.EXECUTE_RESOURCE, command));
        model.setStatus("sent command " + command);
        getCapabilities();
    }

    protected void getProtocolState() {
        String state = sendCommand(buildCommand(DriverCommandEnum.GET_RESOURCE_STATE), 5);
        model.setState(state);
    }

    protected void getResource(String... resources) {
        String reply = sendCommand(buildCommand(DriverCommandEnum.GET_RESOURCE, resources));
        if (reply != null) {
            model.setParams(new JSONObject(reply));
        }
    }

    protected void setResource(String parameters) {
        sendCommand(buildCommand(DriverCommandEnum.SET_RESOURCE, parameters));
    }

    private String sendCommand(JSONObject command) {
        return sendCommand(command, 600);
    }

    private String sendCommand(JSONObject command, long timeout) {
        synchronized (command_socket) {
            command_socket.send(command.toString());
            String reply = null;

            // loop on the command socket for a response
            Instant endTime = Instant.now().plusSeconds(timeout);
            while (Instant.now().isBefore(endTime)) {
                ZMsg msg = ZMsg.recvMsg(command_socket, ZMQ.NOBLOCK);
                if (msg != null) {
                    reply = msg.popString();
                    break;
                }
                try { Thread.sleep(100); } catch (InterruptedException ignored) { }
            }

            if (reply != null) {
                log.debug("received reply: {}", reply);
                if (reply.startsWith("[")) {
                    JSONArray possibleException = new JSONArray(reply);
                    if (possibleException.length() == 3) {
                        log.error("EXCEPTION FROM DRIVER: {}", possibleException);
                        return null;
                    }
                }
                if (reply.startsWith("\"")) {
                    reply = reply.substring(1, reply.length() - 1);
                }
            } else {
                log.debug("no reply received!");
            }
            return reply;
        }
    }

    private JSONObject buildCommand(DriverCommandEnum command, String... args) {
        JSONObject message = new JSONObject();
        JSONObject keyword_args = new JSONObject();
        JSONArray message_args = new JSONArray();

        for (String arg: args) {
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
