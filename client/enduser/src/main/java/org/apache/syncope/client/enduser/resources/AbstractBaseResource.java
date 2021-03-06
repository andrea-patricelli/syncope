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

import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBaseResource extends AbstractResource {

    private static final long serialVersionUID = -7875801358718612782L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseResource.class);

    protected <T> T getService(final Class<T> serviceClass) {
        return SyncopeEnduserSession.get().getService(serviceClass);
    }

    protected <T> T getService(final String etag, final Class<T> serviceClass) {
        return SyncopeEnduserSession.get().getService(etag, serviceClass);
    }

    protected <T> void resetClient(final Class<T> serviceClass) {
        SyncopeEnduserSession.get().resetClient(serviceClass);
    }

    protected boolean isSelfRegistrationAllowed() {
        Boolean result = null;
        try {
            result = SyncopeEnduserSession.get().getSyncopeTO().isSelfRegAllowed();
        } catch (SyncopeClientException e) {
            LOG.error("While seeking if self registration is allowed", e);
        }

        return result == null
                ? false
                : result;
    }

}
