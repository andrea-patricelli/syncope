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
package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public abstract class AbstractAnyRestClient extends BaseRestClient {

    private static final long serialVersionUID = 1962529678091410544L;

    public abstract int count(String realm);

    public abstract List<? extends AnyTO> list(
            String realm, int page, int size, final SortParam<String> sort, final String type);

    public abstract int searchCount(String realm, String fiql, final String type);

    public abstract List<? extends AnyTO> search(
            String realm, String fiql, int page, int size, final SortParam<String> sort, final String type);

    public abstract ConnObjectTO readConnObject(String resourceName, Long key);

    public abstract AnyTO delete(String etag, Long key);

    public abstract BulkActionResult bulkAction(BulkAction action);

    protected abstract Class<? extends AnyService<?, ?>> getAnyServiceClass();

    public void unlink(final String etag, final long key, final List<StatusBean> statuses) {
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            DeassociationPatch deassociationPatch = new DeassociationPatch();
            deassociationPatch.setKey(key);
            deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
            deassociationPatch.getResources().addAll(StatusUtils.buildStatusPatch(statuses).getResources());

            service.deassociate(deassociationPatch);

            resetClient(getAnyServiceClass());
        }
    }

    public void link(final String etag, final long key, final List<StatusBean> statuses) {
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses);

            AssociationPatch associationPatch = new AssociationPatch();
            associationPatch.setKey(key);
            associationPatch.setAction(ResourceAssociationAction.LINK);
            associationPatch.setOnSyncope(statusPatch.isOnSyncope());
            associationPatch.getResources().addAll(statusPatch.getResources());

            service.associate(associationPatch);

            resetClient(getAnyServiceClass());
        }
    }

    public BulkActionResult deprovision(final String etag, final long key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            DeassociationPatch deassociationPatch = new DeassociationPatch();
            deassociationPatch.setKey(key);
            deassociationPatch.setAction(ResourceDeassociationAction.DEPROVISION);
            deassociationPatch.getResources().addAll(StatusUtils.buildStatusPatch(statuses).getResources());

            result = service.deassociate(deassociationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final long key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses);

            AssociationPatch associationPatch = new AssociationPatch();
            associationPatch.setKey(key);
            associationPatch.setAction(ResourceAssociationAction.PROVISION);
            associationPatch.setOnSyncope(statusPatch.isOnSyncope());
            associationPatch.getResources().addAll(statusPatch.getResources());

            result = service.associate(associationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final long key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            DeassociationPatch deassociationPatch = new DeassociationPatch();
            deassociationPatch.setKey(key);
            deassociationPatch.setAction(ResourceDeassociationAction.UNASSIGN);
            deassociationPatch.getResources().addAll(StatusUtils.buildStatusPatch(statuses).getResources());

            result = service.deassociate(deassociationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final long key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses);

            AssociationPatch associationPatch = new AssociationPatch();
            associationPatch.setKey(key);
            associationPatch.setAction(ResourceAssociationAction.ASSIGN);
            associationPatch.setOnSyncope(statusPatch.isOnSyncope());
            associationPatch.getResources().addAll(statusPatch.getResources());

            result = service.associate(associationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

}
