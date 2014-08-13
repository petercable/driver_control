package com.raytheon.ooi.driver_control;

public class DriverControl {
//    private static Logger log = LogManager.getLogger("DriverControl");
//    private DriverModel model;
//    private volatile boolean isCommanding = false;
//    private ExecutorService executor;
//    private ScheduledExecutorService scheduler;
//    private final String busy = "busy processing previous command";
//    private DriverInterface driverInterface;
//
//    public DriverControl(DriverInterface driverInterface, DriverModel model) {
//        this.model = model;
//        this.driverInterface = driverInterface;
//        log.debug("Initialize DriverControl");
//
////        BasicThreadFactory factory = new BasicThreadFactory.Builder()
////                .namingPattern("controllerPool-%d")
////                .daemon(true)
////                .priority(Thread.NORM_PRIORITY)
////                .build();
////        BasicThreadFactory scheduleFactory = new BasicThreadFactory.Builder()
////                .namingPattern("controllerScheduler")
////                .daemon(true)
////                .priority(Thread.NORM_PRIORITY)
////                .build();
////        executor = Executors.newCachedThreadPool(factory);
////        getMetadata();
////        scheduler = Executors.newSingleThreadScheduledExecutor(scheduleFactory);
////        scheduler.scheduleAtFixedRate(this::ping, 0, 10, TimeUnit.SECONDS);
////        scheduler.scheduleAtFixedRate(this::getCapabilities, 3, 10, TimeUnit.SECONDS);
////        scheduler.scheduleAtFixedRate(this::getProtocolState, 6, 10, TimeUnit.SECONDS);
////        Platform.runLater(() -> model.setConnection("CONNECTED"));
//    }
//
//    protected String ping() {
//        return driverInterface.ping();
//    }
//
//    protected String configure() {
//        sendCommand(DriverCommandEnum.CONFIGURE, 15, model.getConfig().getPortAgentConfig());
//    }
//
//    protected String init() {
//        sendCommand(DriverCommandEnum.SET_INIT_PARAMS, 5, model.getConfig().getStartupConfig());
//    }
//
//    protected String connect() {
//        sendCommand(DriverCommandEnum.CONNECT, 15);
//    }
//
//    protected String discover() {
//        sendCommand(DriverCommandEnum.DISCOVER_STATE);
//    }
//
//    protected String stop() {
//        sendCommand(DriverCommandEnum.STOP_DRIVER, 5);
//    }
//
//    protected String getMetadata() {
//        sendCommand(DriverCommandEnum.GET_CONFIG_METADATA, 5);
//    }
//
//    protected String getCapabilities() {
//        sendCommand(DriverCommandEnum.GET_CAPABILITIES, 5);
//    }
//
//    protected String execute(String command) {
//        sendCommand(DriverCommandEnum.EXECUTE_RESOURCE, command);
//    }
//
//    protected String getProtocolState() {
//        sendCommand(DriverCommandEnum.GET_RESOURCE_STATE, 5);
//    }
//
//    protected String getResource(String... resources) {
//        sendCommand(DriverCommandEnum.GET_RESOURCE, resources);
//    }
//
//    protected String setResource(String parameters) {
//        sendCommand(DriverCommandEnum.SET_RESOURCE, parameters);
//    }
//
//    private Future<String> sendCommand(DriverCommandEnum command, Supplier<String> function) {
//        if (isCommanding) {
//            return executor.submit(() -> busy);
//        }
//
//        isCommanding = true;
//        Platform.runLater(()->model.setConnection(String.format("BUSY (%s)", command)));
//        return executor.submit(function::get);
//    }
//
//    private String _sendCommand(DriverCommandEnum command, int timeout, String... args) {
//        Platform.runLater(() -> model.setStatus(String.format("sending command %s...", command.toString())));
//        String reply = null;
//
//        try {
//            command_socket.send(buildCommand(command, args).toString());
//            command_socket.setReceiveTimeOut(timeout * 1000);
//            reply = command_socket.recvStr();
//        } finally {
//            isCommanding = false;
//            Platform.runLater(()->model.setConnection("IDLE"));
//        }
//
//        log.debug("receive loop complete, reply: {}", reply);
//
//        if (reply == null) {
//            // No response from instrument, set to disconnected
//            Platform.runLater(()-> {
//                model.setStatus("NO REPLY FROM INSTRUMENT");
//                model.setConnection("DISCONNECTED");
//            });
//        } else {
//            log.debug("received reply: {}", reply);
//            if (reply.startsWith("[")) {
//                JSONArray possibleException = new JSONArray(reply);
//                if (possibleException.length() == 3) {
//                    log.error("EXCEPTION FROM DRIVER: {}", possibleException);
//                    return reply;
//                }
//            }
//            if (reply.startsWith("\"")) {
//                reply = reply.substring(1, reply.length() - 1);
//            }
//            switch (command) {
//                case GET_CONFIG_METADATA:
//                    final JSONObject metaData = new JSONObject(reply.replace("\\\"", "\""));
//                    Platform.runLater(() -> {
//                        model.parseMetadata(metaData);
//                        model.setStatus("received meta data");
//                    });
//                    break;
//                case GET_CAPABILITIES:
//                    final JSONArray capes = new JSONArray(reply).getJSONArray(0);
//                    Platform.runLater(() -> {
//                        model.parseCapabilities(capes);
//                        model.setStatus("received capabilities");
//                    });
//                    break;
//                case GET_RESOURCE_STATE:
//                    final String state = reply;
//                    Platform.runLater(() -> {
//                        model.setState(state);
//                        model.setStatus("received state");
//                    });
//                    break;
//                case GET_RESOURCE:
//                    final JSONObject resource = new JSONObject(reply);
//                    Platform.runLater(() -> {
//                        model.setParams(resource);
//                        model.setStatus("received resource");
//                    });
//                    break;
//                default:
//                    log.trace("hit default in switch statement");
//                    Platform.runLater(() -> model.setStatus(""));
//                    break;
//            }
//        }
//        return reply;
//    }
//
//    private Future<String> sendCommand(DriverCommandEnum command, String... args) {
//        return sendCommand(command, 600, args);
//    }
//
//    private Future<String> sendCommand(DriverCommandEnum command, int timeout, String... args) {
//        log.debug("sending command to driver {}", command);
//        if (isCommanding) {
//            String status = String.format(
//                    "unable to send command %s while processing previous command",
//                    command.toString());
//            log.debug(status);
//            Platform.runLater(() -> model.setStatus(status));
//            return executor.submit(() -> busy);
//        }
//
//        isCommanding = true;
//        Platform.runLater(()->model.setConnection(String.format("BUSY (%s)", command.toString())));
//        return executor.submit(() -> this._sendCommand(command, timeout, args));
//    }
//
//    private JSONObject buildCommand(DriverCommandEnum command, String... args) {
//        JSONObject message = new JSONObject();
//        JSONObject keyword_args = new JSONObject();
//        JSONArray message_args = new JSONArray();
//
//        for (String arg : args) {
//            try {
//                message_args.put(new JSONObject(arg));
//            } catch (JSONException e) {
//                message_args.put(arg);
//            }
//        }
//        message.put("cmd", command);
//        message.put("args", message_args);
//        message.put("kwargs", keyword_args);
//        log.debug("BUILT COMMAND: {}", message);
//        return message;
//    }
}
