package com.raytheon.ooi.driver_control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.StringJoiner;

public class DriverConfig {
    private final static Logger log = LogManager.getLogger(DriverConfig.class);
    private Map portAgentConfig;
    private Map startupConfig;
    private String scenario;

    private final String host = "localhost";
    private final String workDir = "/tmp/driver_control";
    private final String commandPortFile = String.join("/", workDir, "command_port");
    private final String eventPortFile = String.join("/", workDir, "event_port");
    private final String databaseFile = String.join("/", workDir, "preload.db");

    public DriverConfig(File file) throws IOException {
        // open the file, parse the config
        Path path = Paths.get(file.toURI());
        Yaml yaml = new Yaml();
        Map map = (Map) yaml.load(Files.newInputStream(path));

        portAgentConfig = (Map) map.get("port_agent_config");
        startupConfig = (Map) map.get("startup_config");
        Map driverConfig = (Map) map.get("driver_config");
        scenario = (String) driverConfig.get("scenario");
    }

    public Map getPortAgentConfig() {
        return portAgentConfig;
    }

    public Map getStartupConfig() {
        return startupConfig;
    }

    public String getCommandPortFile() {
        return commandPortFile;
    }

    public String getEventPortFile() {
        return eventPortFile;
    }

    public String toString() {
        StringJoiner joiner = new StringJoiner("\n\n");
        joiner.add("PORT AGENT CONFIG");
        joiner.add(portAgentConfig.toString());
        joiner.add("STARTUP CONFIG");
        joiner.add(startupConfig.toString());
        joiner.add("COMMAND PORT FILE: " + commandPortFile);
        joiner.add("EVENT PORT FILE: " + eventPortFile);
        return joiner.toString();
    }

    public String getHost() {
        return host;
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    public String getScenario() {
        return scenario;
    }

    public String getWorkDir() {
        return workDir;
    }

    private int getPort(String filename) throws IOException {
        Path path = Paths.get(filename);
        String contents = new String(Files.readAllBytes(path));
        return Integer.parseInt(contents.trim());
    }

    public int getCommandPort() throws IOException {
        return getPort(commandPortFile);
    }

    public int getEventPort() throws IOException {
        return getPort(eventPortFile);
    }
}
