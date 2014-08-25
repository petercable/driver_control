package com.raytheon.ooi.driver_control;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DriverConfig {
    private Map portAgentConfig;
    private Map startupConfig;
    private String scenario;

    private final String workDir = System.getProperty("workDir");
    private String commandPortFile;
    private String eventPortFile;
    private String scenarioDir;
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

        scenarioDir = String.join("/", workDir, scenario);
        commandPortFile = String.join("/", scenarioDir, "command_port");
        eventPortFile = String.join("/", scenarioDir, "event_port");
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

    public String getHost() {
        return "localhost";
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

    public String getScenarioDir() {
        return scenarioDir;
    }
}
