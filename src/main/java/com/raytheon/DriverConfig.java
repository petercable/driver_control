package com.raytheon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DriverConfig {
    private static Logger log = LogManager.getLogger();
    private JSONObject portAgentConfig;
    private JSONObject startupConfig;
    private URI eggUrl;
    private String commandPortFile;
    private String eventPortFile;
    private String host;

    public DriverConfig(File file) throws IOException, URISyntaxException {
        // open the file, parse the config
        Path path = Paths.get(file.toURI());
        Yaml yaml = new Yaml();
        Map map = (Map) yaml.load(Files.newInputStream(path));
        JSONObject config = new JSONObject(map);

        portAgentConfig = config.getJSONObject("port_agent_config");
        startupConfig = config.getJSONObject("startup_config");
        JSONObject driverConfig = config.getJSONObject("driver_config");
        eggUrl = new URI(driverConfig.getString("egg_url"));
        commandPortFile = driverConfig.getString("command_port_file");
        eventPortFile = driverConfig.getString("event_port_file");
        host = driverConfig.getString("driver_host");
    }

    public String getPortAgentConfig() {
        return portAgentConfig.toString();
    }

    public String getStartupConfig() {
        return startupConfig.toString();
    }

    public URI getEggUri() throws URISyntaxException {
        return eggUrl;
    }

    public String getCommandPortFile() {
        return commandPortFile;
    }

    public String getEventPortFile() {
        return eventPortFile;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PORT AGENT CONFIG\n\n");
        sb.append(portAgentConfig.toString(2));
        sb.append("\n\nSTARTUP CONFIG\n\n");
        sb.append(startupConfig.toString(2));
        sb.append("\n\nEGG URL: ");
        sb.append(eggUrl.toString());
        sb.append("\nCOMMAND PORT FILE: ");
        sb.append(commandPortFile);
        sb.append("\nEVENT PORT FILE: ");
        sb.append(eventPortFile);
        return sb.toString();
    }

    public String getHost() {
        return host;
    }
}
