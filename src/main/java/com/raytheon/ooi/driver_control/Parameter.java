package com.raytheon.ooi.driver_control;

import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parameter {
    private SimpleStringProperty name;
    private SimpleStringProperty displayName;
    private SimpleStringProperty description;
    private SimpleStringProperty visibility;
    private SimpleStringProperty startup;
    private SimpleStringProperty directAccess;
    private SimpleStringProperty valueDescription;
    private SimpleStringProperty value;
    private SimpleStringProperty newValue;
    private SimpleStringProperty valueType;
    private SimpleStringProperty units;
    private static Logger log = LoggerFactory.getLogger(Parameter.class);

    public Parameter(String name, String displayName, String desc, String vis, String startup, String directAccess,
                     String val_desc, String valType, String units) {
        this.name = new SimpleStringProperty(name);
        this.displayName = new SimpleStringProperty(displayName);
        this.description = new SimpleStringProperty(desc);
        this.visibility = new SimpleStringProperty(vis);
        this.startup = new SimpleStringProperty(startup);
        this.directAccess = new SimpleStringProperty(directAccess);
        this.valueDescription = new SimpleStringProperty(val_desc);
        this.units = new SimpleStringProperty(units);
        this.valueType = new SimpleStringProperty(valType);
        this.value = new SimpleStringProperty("");
        this.newValue = new SimpleStringProperty("");
    }

    public String getName() { return name.get(); }

    public String getDisplayName() { return displayName.get(); }

    public String getDescription() { return description.get(); }

    public String getVisibility() { return visibility.get(); }

    public String getValueDescription() { return valueDescription.get(); }

    public String getValue() { return value.get(); }

    public void setValue(String value) { this.value.set(value); }

    public String getValueType() { return valueType.get(); }

    public void setNewValue(String value) { this.newValue.set(value); }

    public String getNewValue() { return newValue.get(); }

    public String getUnits() { return units.get(); }

    public String toString() {
        return String.format("name: %s displayName: %s", name, displayName);
    }

    public SimpleStringProperty valueProperty() { return value; }

    public SimpleStringProperty nameProperty() { return name; }

    public SimpleStringProperty newValueProperty() { return newValue; }

    public String getStartup() { return startup.get(); }

    public String getDirectAccess() { return directAccess.get(); }

    public SimpleStringProperty startupProperty() { return startup; }

    public SimpleStringProperty directAccessProperty() { return directAccess; }
}
