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
package org.apache.syncope.core.rest.cxf.service;

import java.net.URI;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.JWSAlgorithm;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.OIDCJWKSLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.syncope.common.rest.api.service.OIDCJWKSService;

@Service
public class OIDCJWKSServiceImpl extends AbstractServiceImpl implements OIDCJWKSService {

    @Autowired
    private OIDCJWKSLogic logic;

    @Override
    public OIDCJWKSTO read() {
        return logic.read();
    }

    @Override
    public Response generate(final int size, final JWSAlgorithm algorithm) {
        OIDCJWKSTO jwks = logic.generate(size, algorithm);
        URI location = uriInfo.getAbsolutePathBuilder().path(jwks.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, jwks.getKey()).
                entity(jwks).
                build();
    }

    @Override
    public void delete() {
        logic.delete();
    }
}