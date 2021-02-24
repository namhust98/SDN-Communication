package org.onosproject.sina.locallistener;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.sina.restapi.SinaApiManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true)
public class SinaListenerManager {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    public static final String PROVIDER_NAME = "org.onosproject.sina.listener";

    private final LocalDeviceListener deviceListener = new LocalDeviceListener();
    private final LocalHostListener hostListener = new LocalHostListener();
    private final LocalLinkListener linkListener = new LocalLinkListener();

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);

        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        log.info("Started");

        createListIpFile();
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);

        log.info("Stopped");
    }

    private void createListIpFile() {
        try {
            String path = System.getProperty("java.io.tmpdir");
            path = path + "/listip.json";
            FileOutputStream fos = new FileOutputStream(path);

            OutputStreamWriter wrt = new OutputStreamWriter(fos);
            wrt.write("{\n" +
                    "\t\"localIp\": \"...\",\n" +
                    "\t\"controller\": \"...\", \n" +
                    "\t\"communication\": [\n" +
                    "\t\t{\n" +
                    "\t\t\t\"ip\": \"...\", \n" +
                    "\t\t\t\"controller\": \"...\"\n" +
                    "\t\t}, \n" +
                    "\t\t{\n" +
                    "\t\t\t\"ip\": \"...\", \n" +
                    "\t\t\t\"controller\": \"...\"\n" +
                    "\t\t}\n" +
                    "\t]\n" +
                    "}");
            wrt.close();
            fos.close();
        } catch (IOException e) {
            log.info("Error when create file listip.json");
        }
    }

    private class LocalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            SinaApiManager sinaApiManager = new SinaApiManager();
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    JSONObject object = sinaApiManager.getDevices();
                    sendNotify("devices", object.toString());
                    break;
                case PORT_REMOVED:
                case PORT_ADDED:
                    JSONObject object1 = sinaApiManager.getPorts();
                    sendNotify("ports", object1.toString());
                    break;
                default:
                    break;
            }
        }
    }

    private class LocalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            SinaApiManager sinaApiManager = new SinaApiManager();
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_REMOVED:
                    JSONObject object = sinaApiManager.getHosts();
                    sendNotify("hosts", object.toString());
                    break;
                default:
                    break;
            }
        }
    }

    private class LocalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            SinaApiManager sinaApiManager = new SinaApiManager();
            switch (event.type()) {
                case LINK_ADDED:
                case LINK_REMOVED:
                case LINK_UPDATED:
                    JSONObject object = sinaApiManager.getLinks();
                    sendNotify("links", object.toString());
                    break;
                default:
                    break;
            }
        }
    }

    private void sendNotify(String kind, String data) {
        try {
            String path = System.getProperty("java.io.tmpdir");

            FileInputStream fis = new FileInputStream(path + "/listip.json");
            InputStreamReader rd = new InputStreamReader(fis);

            BufferedReader br = new BufferedReader(rd);
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                content.append(inputLine);
            }
            br.close();

            JSONObject object = new JSONObject(new String(content));
            String localIp = object.getString("localIp");
            JSONArray array = object.getJSONArray("communication");

            JSONObject dataObject = new JSONObject();
            dataObject.put("ip", localIp);
            dataObject.put("kind", kind);
            dataObject.put("data", data);

            int len = array.length();
            for (int i = 0; i < len; i++) {
                JSONObject controller = array.getJSONObject(i);
                String destIp = controller.getString("ip");
                String kindController = controller.getString("controller");

                switch (kindController) {
                    case "ONOS": {
                        String url = "http://" + destIp + ":8181/onos/sina/updateInfo/updateData";

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> response = Unirest.post(url)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                .body(dataObject.toString())
                                .asString();
                        break;
                    }
                    case "Faucet": {
                        String url = "http://" + destIp + ":8080/faucet/sina/updateInfo/updateData";

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> response = Unirest.post(url)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .body(dataObject.toString())
                                .asString();
                        break;
                    }
                    case "ODL": {
                        final String urlOdl = "http://" + destIp + ":8181/restconf/operations/sina:updateData";

                        JSONObject dataForm = new JSONObject();
                        JSONObject dataFinal = new JSONObject();
                        dataForm.put("data", dataObject.toString());
                        dataFinal.put("input", dataForm);

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> response = Unirest.post(urlOdl)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                                .body(dataFinal)
                                .asString();
                        break;
                    }
                    default:
                        break;
                }
            }


        } catch (IOException e) {
            log.info("Cannot find listip.json file");
        } catch (UnirestException e) {
            log.info("Error when call API");
        }
    }
}