package com.raytheon.ooi.driver_control;

public enum DriverCommandEnum {
    PING                    ("driver_ping"),
    INITIALIZE              ("initialize"),
    CONFIGURE               ("configure"),
    CONNECT                 ("connect"),
    DISCONNECT              ("disconnect"),
    GET_INIT_PARAMS         ("get_init_params"),
    SET_INIT_PARAMS         ("set_init_params"),
    APPLY_STARTUP_PARAMS    ("apply_startup_params"),
    GET_CACHED_CONFIG       ("get_cached_config"),
    GET_CONFIG_METADATA     ("get_config_metadata"),
    DISCOVER_STATE          ("discover_state"),
    GET_RESOURCE_STATE      ("get_resource_state"),
    GET_RESOURCE            ("get_resource"),
    SET_RESOURCE            ("set_resource"),
    START_DIRECT            ("start_direct"),
    EXECUTE_DIRECT          ("execute_direct"),
    STOP_DIRECT             ("stop_direct"),
    GET_CAPABILITIES        ("get_resource_capabilities"),
    EXECUTE_RESOURCE        ("execute_resource"),
    STOP_DRIVER             ("stop_driver_process");

    private final String s;

    DriverCommandEnum(String s) {
        this.s = s;
    }

    public String toString() {
        return s;
    }

    public boolean equals(String s) {
        return s.equals(this.s);
    }
}
