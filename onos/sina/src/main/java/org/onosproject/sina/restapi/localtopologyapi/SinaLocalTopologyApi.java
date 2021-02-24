package org.onosproject.sina.restapi.localtopologyapi;

import org.onlab.rest.BaseResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Sina Local Topology API.
 */
@Path("localTopology")
public class SinaLocalTopologyApi extends BaseResource {

    /**
     * Test Sina Api.
     *
     * @return response code OK
     */
    @GET
    @Path("exampleAPI")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exampleApi() {
        SinaLocalTopologyApiService service = get(SinaLocalTopologyApiService.class);
        return Response.ok(service.exampleApi().toString()).build();
    }

    /**
     * Get list of Devices on local topology.
     *
     * @return response code OK
     */
    @GET
    @Path("getDevices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices() {
        SinaLocalTopologyApiService service = get(SinaLocalTopologyApiService.class);
        return Response.ok(service.getDevices().toString()).build();
    }

    /**
     * Get list of Ports on local topology.
     *
     * @return response code OK
     */
    @GET
    @Path("getPorts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPorts() {
        SinaLocalTopologyApiService service = get(SinaLocalTopologyApiService.class);
        return Response.ok(service.getPorts().toString()).build();
    }

    /**
     * Get list of Hosts on local topology.
     *
     * @return response code OK
     */
    @GET
    @Path("getHosts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHosts() {
        SinaLocalTopologyApiService service = get(SinaLocalTopologyApiService.class);
        return Response.ok(service.getHosts().toString()).build();
    }

    /**
     * Get list of Links on local topology.
     *
     * @return response code OK
     */
    @GET
    @Path("getLinks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinks() {
        SinaLocalTopologyApiService service = get(SinaLocalTopologyApiService.class);
        return Response.ok(service.getLinks().toString()).build();
    }
}
