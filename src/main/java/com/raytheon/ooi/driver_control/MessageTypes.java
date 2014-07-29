package com.raytheon.ooi.driver_control;

public enum MessageTypes {
    SAMPLE        ("DRIVER_ASYNC_EVENT_SAMPLE"),
    STATE_CHANGE  ("DRIVER_ASYNC_EVENT_STATE_CHANGE"),
    CONFIG_CHANGE ("DRIVER_ASYNC_EVENT_CONFIG_CHANGE"),
    ERROR         ("DRIVER_ASYNC_EVENT_ERROR"),
    RESULT        ("DRIVER_ASYNC_RESULT"),
    DIRECT_ACCESS ("DRIVER_ASYNC_EVENT_DIRECT_ACCESS"),
    AGENT_EVENT   ("DRIVER_ASYNC_EVENT_AGENT_EVENT");

    private final String s;

    MessageTypes(String s) {
        this.s = s;
    }

    public String toString() {
        return s;
    }

    public boolean equals(String s) {
        return s.equals(this.s);
    }
}
