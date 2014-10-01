package com.raytheon.ooi.driver_interface;

import com.raytheon.ooi.common.JsonHelper;
import com.raytheon.ooi.driver_control.ControlWindow;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by pcable on 9/25/14.
 */
public class RestDriverCommandInterface {
    private static final Logger log = LoggerFactory.getLogger(ControlWindow.class);

    private Client client;
    private WebTarget target;

    public RestDriverCommandInterface(String id, String uframe_agent_url, String module,
                                      String klass, String host, int commandPort, int eventPort) {
        String info = "Starting agent in uframe: id: " + id + " agent_url: " + uframe_agent_url + " module: " +
                 module + " class: " + klass + " host: " + host + " commandPort: " + commandPort + " eventPort: " + eventPort;
        log.info(info);
        System.out.println(info);
        client = ClientBuilder.newClient();
        target = client.target(uframe_agent_url + "/" + id);
        createAgent(module, klass, host, commandPort, eventPort);
    }

    public static void main(String... args) {
        RestDriverCommandInterface i = new RestDriverCommandInterface("PARAD",
                "http://uframe:12572/instrument/api",
                "mi.instrument.satlantic.par_ser_600m.ooicore.driver",
                "InstrumentDriver",
                "192.168.56.101",
                40001,
                50001);
        System.out.println(i.agentState());
    }

    public String createAgent(String module, String klass, String host, int commandPort, int eventPort) {
        Form form = new Form();
        form.param("module", module);
        form.param("class", klass);
        form.param("host", host);
        form.param("commandPort", Integer.toString(commandPort));
        form.param("eventPort", Integer.toString(eventPort));
        log.info("FORM: " + form.asMap());
        return target.path("").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

    public String deleteAgent() {
        log.info("deleteAgent");
        return target.path("").request(MediaType.APPLICATION_JSON_TYPE).delete(String.class);
    }

    public String agentState() {
        log.info("agentState");
        return target.path("").request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
    }

    public String ping() {
        log.info("ping");
        return target.path("ping")
                .queryParam("timeout", "2000")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
    }

    public String initParams(Map<String, Object> config) throws IOException {
        log.info("initParams");
        Form form = new Form();
        form.param("config", JsonHelper.toJson(config));
        return target.path("initparams")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

    public String configure(Map<String, Object> config) throws IOException {
        log.info("configure: {}", JsonHelper.toJson(config));
        Form form = new Form();
        form.param("config", JsonHelper.toJson(config));
        return target.path("configure")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

    public String connect(int timeout) {
        log.info("connect");
        Form form = new Form();
        form.param("timeout", Integer.toString(timeout));
        return target.path("connect")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

    public String discover(int timeout) {
        log.info("discover");
        Form form = new Form();
        form.param("timeout", Integer.toString(timeout));
        return target.path("discover")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

    public Map<String, Object> metadata() throws IOException {
        log.info("metadata");
        String reply = target.path("metadata").request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        log.info("metadata reply: {}", reply);
        return (Map<String, Object>) JsonHelper.toMap(reply).get("reply");
    }

    public List<String> capabilities() throws IOException {
        log.info("capabilities");
        String reply = target.path("capabilities").request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        log.info("capabilities reply: " + reply);
        return (List<String>) JsonHelper.toMap(reply).get("reply");
    }

    public String state() throws IOException {
        log.info("state");
        String reply = target.path("state").request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        log.info("state reply: " + reply);
        return (String) JsonHelper.toMap(reply).get("reply");
    }

    public Map<String, Object> getResource(String resource, int timeout) throws IOException {
        log.info("getResource");
        String reply = target.path("resource")
                .queryParam("resource", resource)
                .queryParam("timeout", timeout)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
        log.info("getResource reply: {}", reply);
        return (Map<String, Object>) JsonHelper.toMap(reply).get("reply");
    }

    public String setResource(Map<String, Object> resource, int timeout) throws IOException {
        Form form = new Form();
        form.param("resource", JsonHelper.toJson(resource));
        form.param("timeout", Integer.toString(timeout));
        return target.path("resource")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

    public String execute(String command, String kwargs, int timeout) {
        Form form = new Form();
        form.param("command", command);
        form.param("kwargs", kwargs);
        form.param("timeout", Integer.toString(timeout));
        return target.path("execute")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
    }

}
