/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.rest.api.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;

/**
 * REST operations for connector configuration versioning.
 */
@Api(tags = "ConnectorHistory", authorizations = {
    @Authorization(value = "BasicAuthentication")
    , @Authorization(value = "Bearer") })
@Path("connectorsHistory")
public interface ConnectorHistoryService extends JAXRSService {

    /**
     * Returns a list of all connector configuration history instances for the given connector instance key.
     *
     * @param connectorKey connector instance key
     * @return list of all connector configuration history instances for the given connector instance key
     */
    @GET
    @Path("{connectorKey}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ConnInstanceHistoryConfTO> list(@NotNull @PathParam("connectorKey") String connectorKey);

    /**
     * Restores the connector configuration history instance matching the provided key.
     *
     * @param key connector configuration history instance key to be restored
     * @return an empty response if operation was successful
     */
    @POST
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response restore(@NotNull @PathParam("key") String key);

    /**
     * Deletes the connector configuration history instance matching the provided key.
     *
     * @param key connector configuration history instance key to be deleted
     * @return an empty response if operation was successful
     */
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response delete(@NotNull @PathParam("key") String key);
}
