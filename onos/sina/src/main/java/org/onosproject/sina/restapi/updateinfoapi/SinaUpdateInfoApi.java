package org.onosproject.sina.restapi.updateinfoapi;

import org.onlab.rest.BaseResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Sina Update Information of other controller API.
 */
@Path("updateInfo")
public class SinaUpdateInfoApi extends BaseResource {

    /**
     * Update information about other controller.
     *
     * @param data String data to update
     * @return response code OK
     */
    @POST
    @Path("updateData")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateData(String data) {
        SinaUpdateInfoApiService service = get(SinaUpdateInfoApiService.class);
        return Response.ok(service.updateData(data).toString()).build();
    }
}
