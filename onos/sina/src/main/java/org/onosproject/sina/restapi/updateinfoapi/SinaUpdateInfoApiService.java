package org.onosproject.sina.restapi.updateinfoapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Sina update information api interface.
 */
public interface SinaUpdateInfoApiService {

    /**
     * Update information about other controller.
     *
     * @param data String data to update
     * @return JSON representation
     */
    JsonNode updateData(String data);
}
