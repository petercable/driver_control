package com.raytheon.ooi.common;

/**
 * Created by pcable on 8/15/14.
 */
public class Constants {
    private Constants() {}

    // Driver Commands
    public final static String PING                     = "driver_ping";
    public final static String INITIALIZE               = "initParams";
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

    // Driver States
    public final static String DRIVER_STATE_UNCONFIGURED = "DRIVER_STATE_UNCONFIGURED";
    public final static String DRIVER_STATE_DISCONNECTED = "DRIVER_STATE_DISCONNECTED";
    public final static String DRIVER_STATE_UNKNOWN      = "DRIVER_STATE_UNKNOWN";

    // Driver Events
    public final static String STATE_CHANGE_EVENT       = "DRIVER_ASYNC_EVENT_STATE_CHANGE";
    public final static String SAMPLE_EVENT             = "DRIVER_ASYNC_EVENT_SAMPLE";
    public final static String CONFIG_CHANGE_EVENT      = "DRIVER_ASYNC_EVENT_CONFIG_CHANGE";

    // Driver Streams
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

    // Preload
    public static final String PARAMETER_TYPE_QUANTITY  = "quantity";
    public static final String PARAMETER_TYPE_FUNCTION  = "function";
    public static final String PARAMETER_TYPE_ARRAY     = "array<quantity>";
    public static final String PARAMETER_TYPE_CATEGORY  = "category<int8:str>";
    public static final String PARAMETER_TYPE_BOOLEAN   = "boolean";
    public static final String VALUE_TYPE_INT8          = "int8";
    public static final String VALUE_TYPE_INT16         = "int16";
    public static final String VALUE_TYPE_INT32         = "int32";
    public static final String VALUE_TYPE_INT64         = "int64";
    public static final String VALUE_TYPE_UINT8         = "uint8";
    public static final String VALUE_TYPE_UINT16        = "uint16";
    public static final String VALUE_TYPE_UINT32        = "uint32";
    public static final String VALUE_TYPE_UINT64        = "uint64";
    public static final String VALUE_TYPE_FLOAT32       = "float32";
    public static final String VALUE_TYPE_FLOAT64       = "float64";
    public static final String VALUE_TYPE_STR           = "str";
}
