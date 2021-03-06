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
package org.apache.syncope.client.enduser.resources;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.model.Credentials;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginResource extends AbstractBaseResource {

    private static final long serialVersionUID = -7720997467070461915L;

    private static final Logger LOG = LoggerFactory.getLogger(LoginResource.class);

    public LoginResource() {
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {

        int responseStatus;
        final String responseMessage;
        ResourceResponse response = new ResourceResponse();

        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            Credentials credentials = POJOHelper.deserialize(IOUtils.toString(request.getInputStream()),
                    Credentials.class);
            final String username = credentials.getUsername();
            final String password = credentials.getPassword().isEmpty() ? null : credentials.getPassword();

            LOG.debug("Enduser login, user: {}", username);

            if (StringUtils.isBlank(username)) {
                LOG.error("Could not read credentials from request: username is blank!");
                responseMessage = "Could not read credentials from request: username is blank!";
                responseStatus = 400;
            } else {
                // authenticate user
                final boolean authenticated = SyncopeEnduserSession.get().authenticate(username, password);
                responseStatus = authenticated ? 200 : 401;
                responseMessage = username;
            }

            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

        } catch (Exception e) {
            responseStatus = 400;
            LOG.error("Could not read credentials from request", e);
        }

        response.setStatusCode(responseStatus);
        return response;
    }

}
