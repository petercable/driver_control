package com.raytheon.ooi.driver_control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DriverConfig {
    private static final Logger log = LoggerFactory.getLogger(ControlWindow.class);

    private Map portAgentConfig;
    private Map startupConfig;
    private String scenario;
    private String module;
    private String klass;
    private String host;
    private String uframe_agent_url;
    private int commandPort;
    private int eventPort;

    private final String workDir = System.getProperty("workDir");
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
        log.info("driverConfig: " + driverConfig);
        scenario = (String) driverConfig.get("scenario");
        module = (String) driverConfig.get("module");
        klass = (String) driverConfig.get("class");
        log.info("class: " + klass);
        host = (String) driverConfig.get("host");
        uframe_agent_url = (String) driverConfig.get("uframe_agent_url");
        commandPort = (Integer) driverConfig.get("command_port");
        eventPort = (Integer) driverConfig.get("event_port");

        scenarioDir = String.join("/", workDir, scenario);
    }

    public Map getPortAgentConfig() {
        return portAgentConfig;
    }

    public Map getStartupConfig() {
        return startupConfig;
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

    public int getCommandPort() throws IOException {
        return commandPort;
    }

    public int getEventPort() throws IOException {
        return eventPort;
    }

    public String getScenarioDir() {
        return scenarioDir;
    }

    public String getModule() {
        return module;
    }

    public String getKlass() {
        return klass;
    }

    public String getUframe_agent_url() {
        return uframe_agent_url;
    }
}
