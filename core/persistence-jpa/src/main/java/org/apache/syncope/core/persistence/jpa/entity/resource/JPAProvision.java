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
package org.apache.syncope.core.persistence.jpa.entity.resource;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;

@Entity
@Table(name = JPAProvision.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "resource_name", "anyType_name" }))
public class JPAProvision extends AbstractEntity<Long> implements Provision {

    private static final long serialVersionUID = -1807889487945989443L;

    public static final String TABLE = "Provision";

    @Id
    private Long id;

    @ManyToOne
    private JPAExternalResource resource;

    @ManyToOne
    private JPAAnyType anyType;

    @NotNull
    private String objectClass;

    @Lob
    private String serializedSyncToken;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "provision")
    private JPAMapping mapping;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        this.resource = (JPAExternalResource) resource;
    }

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
    }

    @Override
    public ObjectClass getObjectClass() {
        return objectClass == null
                ? null
                : new ObjectClass(objectClass);
    }

    @Override
    public void setObjectClass(final ObjectClass objectClass) {
        this.objectClass = objectClass == null ? null : objectClass.getObjectClassValue();
    }

    @Override
    public SyncToken getSyncToken() {
        return serializedSyncToken == null
                ? null
                : POJOHelper.deserialize(serializedSyncToken, SyncToken.class);
    }

    @Override
    public String getSerializedSyncToken() {
        return this.serializedSyncToken;
    }

    @Override
    public void setSyncToken(final SyncToken syncToken) {
        this.serializedSyncToken = syncToken == null ? null : POJOHelper.serialize(syncToken);
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(final Mapping mapping) {
        checkType(mapping, JPAMapping.class);
        this.mapping = (JPAMapping) mapping;
    }
}
