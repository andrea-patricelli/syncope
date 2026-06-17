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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.lib.to.UserTO;

@Tag(name = "JaversUsers")
@SecurityRequirements({ @SecurityRequirement(name = "BasicAuthentication"), @SecurityRequirement(name = "Bearer") })
@Path("javersUsers")
public interface JaversAuditUserService extends JaversAuditService<UserTO> {

    @Override
    PagedResult<ShadowTO<UserTO>> shadows(String key, int page, int size);

    @Override
    List<ChangesByCommitTO> changes(
            String key,
            String author,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size);
}
