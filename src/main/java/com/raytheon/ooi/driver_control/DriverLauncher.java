package com.raytheon.ooi.driver_control;

import net.lingala.zip4j.exception.ZipException;
import org.apache.logging.log4j.LogManager;
import org.controlsfx.dialog.Dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DriverLauncher {
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    private DriverLauncher() {
    }

    public static Process launchDriver(DriverConfig config)
            throws IOException, ZipException, InterruptedException {
        String scenarioPath = String.join("/", config.getTemp(), config.getScenario());
        String[] env = getEnv(scenarioPath);
        unzipDriver(config.getEggUrl(), scenarioPath);
        patch_zmq_driver(scenarioPath);
        return runDriver(env, scenarioPath, config.getCommandPortFile(), config.getEventPortFile());
    }

    public static String[] getEnv(String scenarioPath) {
        log.debug("Building ENV");
        List<String> eggs = getResourceList("eggs.txt");
        List<String> externs = getResourceList("externs.txt");

        String virtualEnv = System.getenv("VIRTUAL_ENV");
        String workingPath = System.getenv("TEST_BASE");
        if (virtualEnv == null || workingPath == null)
        {
            missingEnvironmentVariable(String.format("VIRTUAL_ENV: %s TEST_BASE: %s", virtualEnv, workingPath));
            return null;
        }
        String path = String.format("PATH=%s/bin:%s", System.getenv("VIRTUAL_ENV"), System.getenv("PATH"));

        StringJoiner joiner = new StringJoiner(":");
        for (String egg: eggs) joiner.add(String.format("%s/eggs/%s", workingPath, egg));
        for (String extern: externs) joiner.add(String.format("%s/extern/%s", workingPath, extern));

        return new String[]{path, String.format("PYTHONPATH=%s:%s", scenarioPath, joiner.toString())};
    }

    public static void missingEnvironmentVariable(String envVar) {
        Dialogs.create()
                .owner(null)
                .title("Environment")
                .message("Missing required environment variable " + envVar)
                .showError();
    }

    public static List<String> getResourceList(String resourceName) {
        log.debug("Getting resource list: {}", resourceName);
        List<String> resources = new LinkedList<>();
        InputStream is = DriverLauncher.class.getClass().getResourceAsStream("/" + resourceName);
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            resources.add(scanner.nextLine());
        }
        return resources;
    }

    public static void patch_zmq_driver(String scenarioPath) throws IOException, InterruptedException {
    /*    Patch an unpacked egg to use JSON instead of PYOBJ.
          assumes we are already in the directory containing the egg to be patched */
        log.debug("Patching zmq_driver to use JSON");
        String[] commands = {
                String.format("sed -i .bak s/pyobj/json/g %s/mi/core/instrument/zmq_driver_process.py", scenarioPath),
                String.format("sed -i .bak s/INFO/DEBUG/g %s/res/config/mi-logging.yml", scenarioPath),
                String.format("cp %s/res/config/mi-logging.yml %s/mi/mi-logging.yml", scenarioPath, scenarioPath)
        };
        Arrays.asList(commands).forEach(log::debug);
        for (String command: commands) {
            log.debug("Executing command {}", command);
            Runtime.getRuntime().exec(command).waitFor();
            log.debug("Done");
        }
    }

    public static void unzipDriver(String eggUrl, String scenarioPath)
            throws IOException, ZipException, InterruptedException {
        log.debug("Unzipping driver");
        Runtime.getRuntime().exec( new String[]{"unzip", "-o", eggUrl, "-d", scenarioPath }).waitFor();
    }

    public static Process runDriver(String[] env, String scenarioPath, String commandPort, String eventPort)
            throws IOException {
        log.debug("Launching driver");
        String[] command =
                { "python", scenarioPath + "/mi/main.py", "--command_port", commandPort, "--event_port", eventPort };
        return Runtime.getRuntime().exec(command, env);
    }
}
