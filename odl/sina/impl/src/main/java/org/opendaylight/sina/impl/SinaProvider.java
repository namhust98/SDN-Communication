/*
 * Copyright © 2018 Copyright (c) 2018 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl;

import com.google.common.util.concurrent.ListenableFuture;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SimpleApiInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SimpleApiOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SimpleApiOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SinaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateDataInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateDataOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateDataOutputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinaProvider implements SinaService, DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(SinaProvider.class);

    private final DataBroker dataBroker;
    private ObjectRegistration<SinaService> sinaService;
    private final RpcProviderService rpcProviderService;
    private ListenerRegistration<?> listenerRegistration;

    final InstanceIdentifier<Node> instanceIdentifier = InstanceIdentifier
            .builder(Nodes.class).child(Node.class).build();

    public SinaProvider(final DataBroker dataBroker, RpcProviderService rpcProviderService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("SinaProvider Session Initiated");
        sinaService = rpcProviderService.registerRpcImplementation(SinaService.class, this);

        listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, instanceIdentifier), this);


        createListIpFile();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("SinaProvider Closed");
        if (sinaService != null) {
            sinaService.close();
        }
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    @Override
    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH"})
    public ListenableFuture<RpcResult<UpdateDataOutput>> updateData(UpdateDataInput input) {
        UpdateDataOutputBuilder updateDataOutputBuilder = new UpdateDataOutputBuilder();
        try {
            assert input.getData() != null;
            JSONObject object = new JSONObject(input.getData());
            String ip = object.getString("ip");
            String kind = object.getString("kind");
            final String dataWrite = object.getString("data");
            String path = System.getProperty("java.io.tmpdir");
            path = path + "/" + ip + "_" + kind + ".json";
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter wrt = new OutputStreamWriter(fos);
            wrt.write(dataWrite);

            wrt.close();
            fos.close();

            updateDataOutputBuilder.setResult("Success");
            LOG.info("Update Data Success");
        } catch (IOException e) {
            String err = e.getMessage();
            updateDataOutputBuilder.setResult(err);
            LOG.info("Update Data Error");
        }
        return RpcResultBuilder.success(updateDataOutputBuilder.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<SimpleApiOutput>> simpleApi(SimpleApiInput input) {
        SimpleApiOutputBuilder helloBuilder = new SimpleApiOutputBuilder();
        helloBuilder.setOut("Hello " + input.getIn());
        LOG.info("Request Api success");
        return RpcResultBuilder.success(helloBuilder.build()).buildFuture();
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Node>> changes) {
        changes.forEach(this::onDataChanged);
    }

    private void onDataChanged(DataTreeModification<Node> change) {
        final DataObjectModification<Node> node = change.getRootNode();
        switch (node.getModificationType()) {
            case DELETE:
                LOG.info("************************************** Node Remove ***************************************");
//                LOG.info("NETCONF Node: {} was removed", node.getIdentifier());
                sendNotify(requestGet());
                break;
            case SUBTREE_MODIFIED:
//                LOG.info("************************************** Node Modify ************************************");
//                LOG.info("NETCONF Node: {} was updated", node.getIdentifier());
                break;
            case WRITE:
                LOG.info("************************************** Node Add *****************************************");
//                LOG.info("NETCONF Node: {} was created", node.getIdentifier());
                sendNotify(requestGet());
                break;
            default:
                throw new IllegalStateException("Unhandled node change" + change);
        }
    }

    @Override
    public void onInitialData() {

    }

    private String requestGet() {
        try {
            Unirest.setTimeouts(0, 0);
            String url = "http://localhost:8181/restconf/operational/network-topology:network-topology/topology/flow:1";
            HttpResponse<String> response;
            response = Unirest.get(url)
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                    .asString();
            JSONObject object = new JSONObject(response);
            return new JSONObject(object.getString("body")).toString();
        } catch (UnirestException e) {
            String error = e.getMessage();
            LOG.error(error);
            return "";
        }
    }

    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING",
            "OS_OPEN_STREAM_EXCEPTION_PATH", "SLF4J_FORMAT_SHOULD_BE_CONST"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING",
            "OS_OPEN_STREAM_EXCEPTION_PATH", "SLF4J_FORMAT_SHOULD_BE_CONST"})
    private void sendNotify(String data) {
        try {
            String path = System.getProperty("java.io.tmpdir");

            FileInputStream fis = new FileInputStream(path + "/listip.json");
            InputStreamReader rd = new InputStreamReader(fis);

            BufferedReader br = new BufferedReader(rd);
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                content.append(inputLine);
            }
            br.close();

            JSONObject object = new JSONObject(new String(content));
            String localIp = object.getString("localIp");
            JSONArray array = object.getJSONArray("communication");

            JSONObject dataObject = new JSONObject();
            dataObject.put("ip", localIp);
            dataObject.put("kind", "topology");
            dataObject.put("data", data);

            int len = array.length();
            for (int i = 0; i < len; i++) {
                JSONObject controller = array.getJSONObject(i);
                String destIp = controller.getString("ip");
                String kindController = controller.getString("controller");

                switch (kindController) {
                    case "ONOS": {
                        final String urlOnos = "http://" + destIp + ":8181/onos/sina/updateInfo/updateData";

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> responseOnos = Unirest.post(urlOnos)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                .body(dataObject)
                                .asString();
                        LOG.info("Send Data to ONOS Success");
                        break;
                    }
                    case "Faucet": {
                        final String urlFaucet = "http://" + destIp + ":8080/faucet/sina/updateInfo/updateData";

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> responseFaucet = Unirest.post(urlFaucet)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .body(dataObject)
                                .asString();
                        LOG.info("Send Data to Faucet Success");
                        break;
                    }
                    case "ODL": {
                        final String urlOdl = "http://" + destIp + ":8181/restconf/operations/sina:updateData";

                        JSONObject dataForm = new JSONObject();
                        JSONObject dataFinal = new JSONObject();
                        dataForm.put("data", dataObject.toString());
                        dataFinal.put("input", dataForm);

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> responseOdl = Unirest.post(urlOdl)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                                .body(dataFinal)
                                .asString();
                        LOG.info("Send Data to ODL Success");
                        break;
                    }
                    default: {
                        LOG.info("Send data Error");
                        break;
                    }
                }
            }


        } catch (IOException e) {
            LOG.error("Cannot find listip.json file");
        } catch (UnirestException e) {
            LOG.error("Error when call API");
        }
    }

    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH"})
    private void createListIpFile() {
        try {
            String path = System.getProperty("java.io.tmpdir");
            path = path + "/listip.json";
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter wrt = new OutputStreamWriter(fos);
            String data = "{\n"
                    + "\t\"localIp\": \"...\",\n"
                    + "\t\"controller\": \"...\",\n"
                    + "\t\"communication\": [\n"
                    + "\t\t{\n"
                    + "\t\t\t\"ip\": \"...\",\n"
                    + "\t\t\t\"controller\": \"...\"\n"
                    + "\t\t},\n"
                    + "\t\t{\n"
                    + "\t\t\t\"ip\": \"...\",\n"
                    + "\t\t\t\"controller\": \"...\"\n"
                    + "\t\t}\n"
                    + "\t]\n"
                    + "}";
            wrt.write(data);
            wrt.close();
            fos.close();
        } catch (IOException e) {
            LOG.error("Error when create file listip.json");
        }
    }
}