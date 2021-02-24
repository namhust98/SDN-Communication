package org.onosproject.sina.restapi.localtopologyapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;

/**
 * Sina topology api interface.
 */
public interface SinaLocalTopologyApiService {

    /**
     * Example API Sina Server.
     *
     * @return JSON representation
     */
    JsonNode exampleApi();

    /**
     * Returns list of Devices on local topology.
     *
     * @return JSON representation
     */
    JSONObject getDevices();

    /**
     * Returns list of Ports on local topology.
     *
     * @return JSON representation
     */
    JSONObject getPorts();

    /**
     * Returns list of Hosts on local topology.
     *
     * @return JSON representation
     */
    JSONObject getHosts();

    /**
     * Get list of Links on a Device.
     *
     * @return JSON representation
     */
    JSONObject getLinks();
}
