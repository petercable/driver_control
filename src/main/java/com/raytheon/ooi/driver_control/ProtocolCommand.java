package com.raytheon.ooi.driver_control;

import javafx.beans.property.SimpleStringProperty;

public class ProtocolCommand {
    private final SimpleStringProperty name;
    private final SimpleStringProperty displayName;

    public ProtocolCommand(String name, String displayName) {
        this.name = new SimpleStringProperty(name);
        this.displayName = new SimpleStringProperty(displayName);
    }

    public String getName() {
        return name.get();
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public String toString() {
        return "name: " + name + " displayName: " + displayName;
    }
}
