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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.utils.ConnObjectUtils;
import org.apache.syncope.core.misc.utils.MappingUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manage the data propagation to external resources.
 */
@Transactional(rollbackFor = { Throwable.class })
public class PropagationManagerImpl implements PropagationManager {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * Group DAO.
     */
    @Autowired
    protected GroupDAO groupDAO;

    /**
     * Resource DAO.
     */
    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected EntityFactory entityFactory;

    /**
     * ConnObjectUtils.
     */
    @Autowired
    protected ConnObjectUtils connObjectUtils;

    @Autowired
    protected MappingUtils mappingUtils;

    protected Any<?, ?> find(final AnyTypeKind kind, final Long key) {
        AnyDAO<? extends Any<?, ?>> dao;
        switch (kind) {
            case ANY_OBJECT:
                dao = anyObjectDAO;
                break;

            case GROUP:
                dao = groupDAO;
                break;

            case USER:
            default:
                dao = userDAO;
        }

        return dao.authFind(key);
    }

    @Override
    public List<PropagationTask> getCreateTasks(
            final AnyTypeKind kind,
            final Long key,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        return getCreateTasks(find(kind, key), null, null, propByRes, vAttrs, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserCreateTasks(
            final Long key,
            final String password,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        return getCreateTasks(userDAO.authFind(key), password, enable, propByRes, vAttrs, noPropResourceNames);
    }

    protected List<PropagationTask> getCreateTasks(
            final Any<?, ?> any,
            final String password,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.<PropagationTask>emptyList();
        }

        if (noPropResourceNames != null) {
            propByRes.get(ResourceOperation.CREATE).removeAll(noPropResourceNames);
        }

        return createTasks(any, password, true, enable, false, propByRes, vAttrs);
    }

    @Override
    public List<PropagationTask> getUpdateTasks(
            final AnyTypeKind kind,
            final Long key,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        return getUpdateTasks(find(kind, key), null, changePwd, enable, propByRes, vAttrs, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(
            final WorkflowResult<Pair<UserPatch, Boolean>> wfResult,
            final boolean changePwd,
            final Collection<String> noPropResourceNames) {

        return getUpdateTasks(
                userDAO.authFind(wfResult.getResult().getKey().getKey()),
                wfResult.getResult().getKey().getPassword() == null
                        ? null
                        : wfResult.getResult().getKey().getPassword().getValue(),
                changePwd,
                wfResult.getResult().getValue(),
                wfResult.getPropByRes(),
                wfResult.getResult().getKey().getVirAttrs(),
                noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(final WorkflowResult<Pair<UserPatch, Boolean>> wfResult) {
        UserPatch userPatch = wfResult.getResult().getKey();

        // Propagate password update only to requested resources
        List<PropagationTask> tasks = new ArrayList<>();
        if (userPatch.getPassword() == null) {
            // a. no specific password propagation request: generate propagation tasks for any resource associated
            tasks = getUserUpdateTasks(wfResult, false, null);
        } else {
            // b. generate the propagation task list in two phases: first the ones containing password,
            // the the rest (with no password)
            PropagationByResource origPropByRes = new PropagationByResource();
            origPropByRes.merge(wfResult.getPropByRes());

            Set<String> pwdResourceNames = new HashSet<>(userPatch.getPassword().getResources());
            Collection<String> currentResourceNames =
                    userDAO.findAllResourceNames(userDAO.authFind(userPatch.getKey()));
            pwdResourceNames.retainAll(currentResourceNames);
            PropagationByResource pwdPropByRes = new PropagationByResource();
            pwdPropByRes.addAll(ResourceOperation.UPDATE, pwdResourceNames);
            if (!pwdPropByRes.isEmpty()) {
                Set<String> toBeExcluded = new HashSet<>(currentResourceNames);
                toBeExcluded.addAll(CollectionUtils.collect(userPatch.getResources(),
                        new Transformer<StringPatchItem, String>() {

                            @Override
                            public String transform(final StringPatchItem input) {
                                return input.getValue();
                            }
                        }));
                toBeExcluded.removeAll(pwdResourceNames);
                tasks.addAll(getUserUpdateTasks(wfResult, true, toBeExcluded));
            }

            PropagationByResource nonPwdPropByRes = new PropagationByResource();
            nonPwdPropByRes.merge(origPropByRes);
            nonPwdPropByRes.removeAll(pwdResourceNames);
            nonPwdPropByRes.purge();
            if (!nonPwdPropByRes.isEmpty()) {
                tasks.addAll(getUserUpdateTasks(wfResult, false, pwdResourceNames));
            }
        }

        return tasks;
    }

    protected List<PropagationTask> getUpdateTasks(
            final Any<?, ?> any,
            final String password,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        if (noPropResourceNames != null) {
            propByRes.removeAll(noPropResourceNames);
        }

        return createTasks(any, password, changePwd, enable, false, propByRes, vAttrs);
    }

    @Override
    public List<PropagationTask> getDeleteTasks(
            final AnyTypeKind kind,
            final Long key,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        Any<?, ?> any = find(kind, key);

        PropagationByResource localPropByRes = new PropagationByResource();

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(ResourceOperation.DELETE, any.getResourceNames());
        } else {
            localPropByRes.merge(propByRes);
        }

        if (noPropResourceNames != null) {
            localPropByRes.removeAll(noPropResourceNames);
        }

        return getDeleteTasks(any, localPropByRes, noPropResourceNames);
    }

    protected List<PropagationTask> getDeleteTasks(
            final Any<?, ?> any,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        return createTasks(any, null, false, false, true, propByRes, null);
    }

    /**
     * Create propagation tasks.
     *
     * @param any to be provisioned
     * @param password clear text password to be provisioned
     * @param changePwd whether password should be included for propagation attributes or not
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether any must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks created
     */
    protected List<PropagationTask> createTasks(final Any<?, ?> any,
            final String password, final boolean changePwd,
            final Boolean enable, final boolean deleteOnResource, final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs) {

        LOG.debug("Provisioning {}:\n{}", any, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge {}:\n{}", any, propByRes);

        // Virtual attributes
        Set<String> virtualResources = new HashSet<>();
        virtualResources.addAll(propByRes.get(ResourceOperation.CREATE));
        virtualResources.addAll(propByRes.get(ResourceOperation.UPDATE));
        if (any instanceof User) {
            virtualResources.addAll(userDAO.findAllResourceNames((User) any));
        } else if (any instanceof AnyObject) {
            virtualResources.addAll(anyObjectDAO.findAllResourceNames((AnyObject) any));
        } else {
            virtualResources.addAll(((Group) any).getResourceNames());
        }

        Map<String, Set<Attribute>> vAttrMap = new HashMap<>();
        for (AttrTO vAttr : CollectionUtils.emptyIfNull(vAttrs)) {
            VirSchema schema = virSchemaDAO.find(vAttr.getSchema());
            if (schema == null) {
                LOG.warn("Ignoring invalid {} {}", VirSchema.class.getSimpleName(), vAttr.getSchema());
            } else if (schema.isReadonly()) {
                LOG.warn("Ignoring read-only {} {}", VirSchema.class.getSimpleName(), vAttr.getSchema());
            } else {
                if (any.getAllowedVirSchemas().contains(schema)
                        && virtualResources.contains(schema.getProvision().getResource().getKey())) {

                    Set<Attribute> values = vAttrMap.get(schema.getProvision().getResource().getKey());
                    if (values == null) {
                        values = new HashSet<>();
                        vAttrMap.put(schema.getProvision().getResource().getKey(), values);
                    }
                    values.add(AttributeBuilder.build(schema.getExtAttrName(), vAttr.getValues()));

                    propByRes.add(ResourceOperation.UPDATE, schema.getProvision().getResource().getKey());
                } else {
                    LOG.warn("{} not owned by or {} not allowed for {}",
                            schema.getProvision().getResource(), schema, any);
                }
            }
        }
        LOG.debug("With virtual attributes {}:\n{}\n{}", any, propByRes, vAttrMap);

        List<PropagationTask> tasks = new ArrayList<>();

        for (Map.Entry<String, ResourceOperation> entry : propByRes.asMap().entrySet()) {
            ExternalResource resource = resourceDAO.find(entry.getKey());
            Provision provision = resource == null ? null : resource.getProvision(any.getType());
            List<MappingItem> mappingItems = provision == null
                    ? Collections.<MappingItem>emptyList()
                    : MappingUtils.getPropagationMappingItems(provision);

            if (resource == null) {
                LOG.error("Invalid resource name specified: {}, ignoring...", entry.getKey());
            } else if (provision == null) {
                LOG.error("No provision specified on resource {} for type {}, ignoring...",
                        resource, any.getType());
            } else if (mappingItems.isEmpty()) {
                LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                        any.getType(), resource);
            } else {
                PropagationTask task = entityFactory.newEntity(PropagationTask.class);
                task.setResource(resource);
                task.setObjectClassName(
                        resource.getProvision(any.getType()).getObjectClass().getObjectClassValue());
                task.setAnyTypeKind(any.getType().getKind());
                task.setAnyType(any.getType().getKey());
                if (!deleteOnResource) {
                    task.setAnyKey(any.getKey());
                }
                task.setOperation(entry.getValue());
                task.setOldConnObjectKey(propByRes.getOldConnObjectKey(resource.getKey()));

                Pair<String, Set<Attribute>> preparedAttrs =
                        mappingUtils.prepareAttrs(any, password, changePwd, enable, provision);
                task.setConnObjectKey(preparedAttrs.getKey());

                // Check if any of mandatory attributes (in the mapping) is missing or not received any value: 
                // if so, add special attributes that will be evaluated by PropagationTaskExecutor
                List<String> mandatoryMissing = new ArrayList<>();
                List<String> mandatoryNullOrEmpty = new ArrayList<>();
                for (MappingItem item : mappingItems) {
                    if (!item.isConnObjectKey()
                            && JexlUtils.evaluateMandatoryCondition(item.getMandatoryCondition(), any)) {

                        Attribute attr = AttributeUtil.find(item.getExtAttrName(), preparedAttrs.getValue());
                        if (attr == null) {
                            mandatoryMissing.add(item.getExtAttrName());
                        } else if (attr.getValue() == null || attr.getValue().isEmpty()) {
                            mandatoryNullOrEmpty.add(item.getExtAttrName());
                        }
                    }
                }
                if (!mandatoryMissing.isEmpty()) {
                    preparedAttrs.getValue().add(AttributeBuilder.build(
                            PropagationTaskExecutor.MANDATORY_MISSING_ATTR_NAME, mandatoryMissing));
                }
                if (!mandatoryNullOrEmpty.isEmpty()) {
                    preparedAttrs.getValue().add(AttributeBuilder.build(
                            PropagationTaskExecutor.MANDATORY_NULL_OR_EMPTY_ATTR_NAME, mandatoryNullOrEmpty));
                }

                if (vAttrMap.containsKey(resource.getKey())) {
                    preparedAttrs.getValue().addAll(vAttrMap.get(resource.getKey()));
                }

                task.setAttributes(preparedAttrs.getValue());

                tasks.add(task);

                LOG.debug("PropagationTask created: {}", task);
            }
        }

        return tasks;
    }
}
