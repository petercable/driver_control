package com.raytheon.ooi.common;

/**
 * Created by pcable on 8/15/14.
 */
public class Constants {
    private Constants() {}

    public final static String PING                     = "driver_ping";
    public final static String INITIALIZE               = "initialize";
    public final static String CONFIGURE                = "configure";
    public final static String CONNECT                  = "connect";
    public final static String DISCONNECT               = "disconnect";
    public final static String GET_INIT_PARAMS          = "get_init_params";
    public final static String SET_INIT_PARAMS          = "set_init_params";
    public final static String APPLY_STARTUP_PARAMS     = "apply_startup_params";
    public final static String GET_CACHED_CONFIG        = "get_cached_config";
    public final static String GET_CONFIG_METADATA      = "get_config_metadata";
    public final static String DISCOVER_STATE           = "discover_state";
    public final static String GET_RESOURCE_STATE       = "get_resource_state";
    public final static String GET_RESOURCE             = "get_resource";
    public final static String SET_RESOURCE             = "set_resource";
    public final static String START_DIRECT             = "start_direct";
    public final static String EXECUTE_DIRECT           = "execute_direct";
    public final static String STOP_DIRECT              = "stop_direct";
    public final static String GET_CAPABILITIES         = "get_resource_capabilities";
    public final static String EXECUTE_RESOURCE         = "execute_resource";
    public final static String STOP_DRIVER              = "stop_driver_process";
    public final static String STATE_CHANGE_EVENT       = "DRIVER_ASYNC_EVENT_STATE_CHANGE";
    public final static String SAMPLE_EVENT             = "DRIVER_ASYNC_EVENT_SAMPLE";
    public final static String CONFIG_CHANGE_EVENT      = "DRIVER_ASYNC_EVENT_CONFIG_CHANGE";
    public static final String STREAM_NAME              = "stream_name";
    public static final String QUALITY_FLAG             = "quality_flag";
    public static final String PREFERRED_TIMESTAMP      = "preferred_timestamp";
    public static final String PORT_TIMESTAMP           = "port_timestamp";
    public static final String DRIVER_TIMESTAMP         = "driver_timestamp";
    public static final String PKT_FORMAT_ID            = "pkt_format_id";
    public static final String PKT_VERSION              = "pkt_version";
    public static final String VALUE                    = "value";
    public static final String VALUES                   = "values";
    public static final String VALUE_ID                 = "value_id";
}
