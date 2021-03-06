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
package org.apache.syncope.fit.core.reference;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.provisioning.api.sync.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.sync.DefaultSyncActions;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Test synchronization action.
 */
public class TestSyncActions extends DefaultSyncActions {

    private int counter = 0;

    @Override
    public <A extends AnyTO> SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final A any)
            throws JobExecutionException {

        AttrTO attrTO = null;
        for (int i = 0; i < any.getPlainAttrs().size(); i++) {
            AttrTO plainAttr = CollectionUtils.get(any.getPlainAttrs(), i);
            if ("fullname".equals(plainAttr.getSchema())) {
                attrTO = plainAttr;
            }
        }
        if (attrTO == null) {
            attrTO = new AttrTO();
            attrTO.setSchema("fullname");
            any.getPlainAttrs().add(attrTO);
        }
        attrTO.getValues().clear();
        attrTO.getValues().add(String.valueOf(counter++));

        return delta;
    }

    @Override
    public <A extends AnyTO> SyncDelta beforeAssign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final A any)
            throws JobExecutionException {

        if (any instanceof UserTO && "test2".equals(UserTO.class.cast(any).getUsername())) {
            throw new IgnoreProvisionException();
        }

        return delta;
    }

    @Override
    public <A extends AnyTO, M extends AnyPatch> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final A any,
            final M anyPatch) throws JobExecutionException {

        AttrPatch fullnamePatch = null;
        for (AttrPatch attrPatch : anyPatch.getPlainAttrs()) {
            if ("fullname".equals(attrPatch.getAttrTO().getSchema())) {
                fullnamePatch = attrPatch;
            }
        }
        if (fullnamePatch == null) {
            fullnamePatch = new AttrPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).
                    attrTO(new AttrTO.Builder().schema("fullname").build()).
                    build();
        }

        fullnamePatch.getAttrTO().getValues().clear();
        fullnamePatch.getAttrTO().getValues().add(String.valueOf(counter++));

        return delta;
    }
}
