package com.raytheon.ooi.driver_control;


public class DataFunction {
    private String id;
    private String name;
    private String function;
    private String owner;
    private String args;

    public DataFunction(String id, String name, String function, String owner, String args) {
        this.id = id;
        this.name = name;
        this.function = function;
        this.owner = owner;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return String.format("ID: %s NAME: %s FUNCTION: %s OWNER: %s ARGS: %s",
                id,
                name,
                function,
                owner,
                args);
    }

    public String getId() {
        return id;
    }

    public String getFunction() {
        return function;
    }

    public String getOwner() {
        return owner;
    }

    public String getArgs() {
        return args;
    }
}
