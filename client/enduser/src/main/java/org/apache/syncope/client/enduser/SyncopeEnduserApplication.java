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
package org.apache.syncope.client.enduser;

import java.io.Serializable;
import org.apache.syncope.client.enduser.pages.HomePage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.syncope.client.enduser.resources.ErrorResource;
import org.apache.syncope.client.enduser.resources.LoginResource;
import org.apache.syncope.client.enduser.resources.LogoutResource;
import org.apache.syncope.client.enduser.resources.SchemaResource;
import org.apache.syncope.client.enduser.resources.SecurityQuestionResource;
import org.apache.syncope.client.enduser.resources.UserSelfCreateResource;
import org.apache.syncope.client.enduser.resources.UserSelfReadResource;
import org.apache.syncope.client.enduser.resources.UserSelfUpdateResource;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeEnduserApplication extends WebApplication implements Serializable {

    private static final long serialVersionUID = -6445919351044845120L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeEnduserApplication.class);

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            new Locale[] {
                Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR")
            }));

    @Override
    protected void init() {
        super.init();

        LOG.debug("init SyncopeEnduserApplication");

        // resource to provide login functionality managed by wicket
        mountResource("/api/login", new ResourceReference("login") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new LoginResource();
            }
        });

        // resource to provide logout functionality managed by wicket
        mountResource("/api/logout", new ResourceReference("logout") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new LogoutResource();
            }
        });

        // resource to retrieve info about logged user
        mountResource("/api/self/read", new ResourceReference("userSelfRead") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfReadResource();
            }
        });

        // resource to provide user self create functionality managed by wicket
        mountResource("/api/self/create", new ResourceReference("userSelfCreate") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfCreateResource();
            }
        });

        // resource to provide user self update functionality managed by wicket
        mountResource("/api/self/update", new ResourceReference("userSelfUpdate") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfUpdateResource();
            }
        });

        mountResource("/api/schemas", new ResourceReference("schemas") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SchemaResource();
            }
        });
        
        mountResource("/api/securityQuestions", new ResourceReference("securityQuestions") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SecurityQuestionResource();
            }
        });

        mountResource("/api/error", new ResourceReference("error") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new ErrorResource();
            }
        });
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeEnduserSession(request);
    }
}
