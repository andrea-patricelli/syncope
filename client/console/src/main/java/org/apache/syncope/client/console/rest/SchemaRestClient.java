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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2479730152700312373L;

    public void filter(
            final List<? extends AbstractSchemaTO> schemaTOs, final Collection<String> allowed, final boolean exclude) {

        for (ListIterator<? extends AbstractSchemaTO> itor = schemaTOs.listIterator(); itor.hasNext();) {
            AbstractSchemaTO schema = itor.next();
            if (exclude) {
                if (!allowed.contains(schema.getKey())) {
                    itor.remove();
                }
            } else {
                if (allowed.contains(schema.getKey())) {
                    itor.remove();
                }
            }
        }
    }

    public List<? extends AbstractSchemaTO> getSchemas(final SchemaType schemaType) {
        List<? extends AbstractSchemaTO> schemas = Collections.emptyList();

        try {
            schemas = getService(SchemaService.class).list(schemaType);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all schemas for {}", schemaType, e);
        }
        return schemas;
    }

    public List<PlainSchemaTO> getSchemas() {
        List<PlainSchemaTO> schemas = null;

        try {
            schemas = getService(SchemaService.class).list(SchemaType.PLAIN);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all schemas", e);
        }

        return schemas;
    }

    public List<String> getSchemaNames(final SchemaType schemaType) {
        List<String> schemaNames = new ArrayList<>();

        try {
            CollectionUtils.collect(getSchemas(schemaType), new Transformer<AbstractSchemaTO, String>() {

                @Override
                public String transform(final AbstractSchemaTO schemaTO) {
                    return schemaTO.getKey();
                }
            }, schemaNames);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    public List<String> getPlainSchemaNames() {
        return getSchemaNames(SchemaType.PLAIN);
    }

    public List<DerSchemaTO> getDerSchemas() {
        List<DerSchemaTO> userDerSchemas = null;

        try {
            userDerSchemas = getService(SchemaService.class).list(SchemaType.DERIVED);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user derived schemas", e);
        }

        return userDerSchemas;
    }

    public List<String> getDerSchemaNames() {
        return getSchemaNames(SchemaType.DERIVED);
    }

    public List<VirSchemaTO> getVirSchemas() {
        List<VirSchemaTO> userVirSchemas = null;

        try {
            userVirSchemas = getService(SchemaService.class).list(SchemaType.VIRTUAL);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all virtual schemas", e);
        }

        return userVirSchemas;
    }

    public List<String> getVirSchemaNames() {
        return getSchemaNames(SchemaType.VIRTUAL);
    }

    public void createPlainSchema(final PlainSchemaTO schemaTO) {
        getService(SchemaService.class).create(SchemaType.PLAIN, schemaTO);
    }

    public PlainSchemaTO readPlainSchema(final String name) {
        PlainSchemaTO schema = null;

        try {
            schema = getService(SchemaService.class).read(SchemaType.PLAIN, name);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a user schema", e);
        }
        return schema;
    }

    public void updatePlainSchema(final PlainSchemaTO schemaTO) {
        getService(SchemaService.class).update(SchemaType.PLAIN, schemaTO);
    }

    public PlainSchemaTO deletePlainSchema(final String name) {
        PlainSchemaTO response = getService(SchemaService.class).read(SchemaType.PLAIN, name);
        getService(SchemaService.class).delete(SchemaType.PLAIN, name);
        return response;
    }

    public void createDerSchema(final DerSchemaTO schemaTO) {
        getService(SchemaService.class).create(SchemaType.DERIVED, schemaTO);
    }

    public DerSchemaTO readDerSchema(final String name) {
        DerSchemaTO derivedSchemaTO = null;
        try {
            derivedSchemaTO = getService(SchemaService.class).read(SchemaType.DERIVED, name);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a derived user schema", e);
        }
        return derivedSchemaTO;
    }

    public void updateVirSchema(final VirSchemaTO schemaTO) {
        getService(SchemaService.class).update(SchemaType.VIRTUAL, schemaTO);
    }

    public DerSchemaTO deleteDerSchema(final String name) {
        DerSchemaTO schemaTO = getService(SchemaService.class).read(SchemaType.DERIVED, name);
        getService(SchemaService.class).delete(SchemaType.DERIVED, name);
        return schemaTO;
    }

    public void createVirSchema(final VirSchemaTO schemaTO) {
        getService(SchemaService.class).create(SchemaType.VIRTUAL, schemaTO);
    }

    public void updateDerSchema(final DerSchemaTO schemaTO) {
        getService(SchemaService.class).update(SchemaType.DERIVED, schemaTO);
    }

    public VirSchemaTO deleteVirSchema(final String name) {
        VirSchemaTO schemaTO = getService(SchemaService.class).read(SchemaType.VIRTUAL, name);
        getService(SchemaService.class).delete(SchemaType.VIRTUAL, name);
        return schemaTO;
    }

    public List<String> getAllValidatorClasses() {
        List<String> response = null;

        try {
            response = SyncopeConsoleSession.get().getSyncopeTO().getValidators();
        } catch (SyncopeClientException e) {
            LOG.error("While getting all validators", e);
        }
        return response;
    }
}
