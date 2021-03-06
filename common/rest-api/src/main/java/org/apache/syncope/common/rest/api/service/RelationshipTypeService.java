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

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;

/**
 * REST operations for relationship types.
 */
@Path("relationshipTypes")
public interface RelationshipTypeService extends JAXRSService {

    /**
     * Returns a list of all relationshipTypes.
     *
     * @return list of all relationshipTypes.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<RelationshipTypeTO> list();

    /**
     * Returns relationshipType with matching key.
     *
     * @param key relationshipType key to be read
     * @return relationshipType with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    RelationshipTypeTO read(@NotNull @PathParam("key") String key);

    /**
     * Creates a new relationshipType.
     *
     * @param relationshipTypeTO relationshipType to be created
     * @return Response object featuring Location header of created relationshipType
     */
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull RelationshipTypeTO relationshipTypeTO);

    /**
     * Updates the relationshipType matching the provided key.
     *
     * @param relationshipTypeTO relationshipType to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull RelationshipTypeTO relationshipTypeTO);

    /**
     * Deletes the relationshipType matching the provided key.
     *
     * @param key relationshipType key to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);
}
