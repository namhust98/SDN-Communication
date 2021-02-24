package org.onosproject.sina.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.onosproject.core.CoreService;
import org.onosproject.sina.restapi.localtopologyapi.SinaLocalTopologyApiService;
import org.onosproject.sina.restapi.updateinfoapi.SinaUpdateInfoApiService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true, service = {SinaLocalTopologyApiService.class, SinaUpdateInfoApiService.class})
public class SinaApiManager implements SinaLocalTopologyApiService, SinaUpdateInfoApiService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    public static final String PROVIDER_NAME = "org.onosproject.sina.api";

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public JsonNode exampleApi() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("available", true);
        return node;
    }

    @Override
    public JsonNode updateData(String data) {
        try {
            JSONObject object = new JSONObject(data);
            String ip = object.getString("ip");
            String kind = object.getString("kind");
            String dataToWrite = object.getString("data");
            String path = System.getProperty("java.io.tmpdir");
            path = path + "/" + ip + "_" + kind + ".json";
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter wrt = new OutputStreamWriter(fos);
            wrt.write(dataToWrite);
            wrt.close();
            fos.close();

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            node.put("Status", "success");
            return node;
        } catch (IOException e) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            node.put("Error", e.getMessage());
            return node;
        }
    }

    @Override
    public JSONObject getDevices() {
            String url = "http://localhost:8181/onos/v1/devices";
            return requestGet(url);
    }

    @Override
    public JSONObject getPorts() {
            String url = "http://localhost:8181/onos/v1/devices/ports";
            return requestGet(url);
    }

    @Override
    public JSONObject getHosts() {
            String url = "http://localhost:8181/onos/v1/hosts";
            return requestGet(url);
    }

    @Override
    public JSONObject getLinks() {
            String url = "http://localhost:8181/onos/v1/links";
            return requestGet(url);
    }

    private static JSONObject requestGet(String url) {
        try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.get(url)
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                    .asString();

            JSONObject object = new JSONObject(response);
            return new JSONObject(object.getString("body"));
        } catch (UnirestException e) {
            return null;
        }
    }
}