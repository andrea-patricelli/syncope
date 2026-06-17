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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ShadowTO;

/**
 * REST operations for audit.
 */
public interface JaversAuditService<TO extends AnyTO> extends JAXRSService {

    /**
     * Returns a list of shadows in javers audit.
     *
     * @param key key of the entity
     * @param page page
     * @param size size
     * @return paged list of changes for the given entity
     */
    @GET
    @Path("shadows/{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PagedResult<ShadowTO<TO>> shadows(@NotNull @PathParam("key") String key, 
            @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue("1") int page,
            @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue("25") int size);

    /**
     * Returns a list of changes in javers audit.
     *
     * @param key key of the entity
     * @param author username of the user that performed the changes on the given entity
     * @param from date from which the changes were performed
     * @param to date to which the changes were performed
     * @param page page
     * @param size size
     * @return paged list of changes for the given entity 
     */
    @GET
    @Path("changes/")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ChangesByCommitTO> changes(@QueryParam("key") String key,
            @QueryParam("author") String author,
            @QueryParam("from") OffsetDateTime from,
            @QueryParam("to") OffsetDateTime to,
            @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue("1") int page,
            @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue("25") int size);

}
