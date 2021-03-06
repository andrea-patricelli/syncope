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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.AuditManager;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.utils.ConnObjectUtils;
import org.apache.syncope.core.misc.utils.ExceptionUtils2;
import org.apache.syncope.core.misc.utils.MappingUtils;
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
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractPropagationTaskExecutor implements PropagationTaskExecutor {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationTaskExecutor.class);

    /**
     * Connector factory.
     */
    @Autowired
    protected ConnectorFactory connFactory;

    /**
     * ConnObjectUtils.
     */
    @Autowired
    protected ConnObjectUtils connObjectUtils;

    /**
     * Any object DAO.
     */
    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * User DAO.
     */
    @Autowired
    protected GroupDAO groupDAO;

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    /**
     * Notification Manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit Manager.
     */
    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected VirAttrCache virAttrCache;

    @Override
    public TaskExec execute(final PropagationTask task) {
        return execute(task, null);
    }

    protected List<PropagationActions> getPropagationActions(final ExternalResource resource) {
        List<PropagationActions> result = new ArrayList<>();

        if (!resource.getPropagationActionsClassNames().isEmpty()) {
            for (String className : resource.getPropagationActionsClassNames()) {
                try {
                    Class<?> actionsClass = Class.forName(className);
                    result.add((PropagationActions) ApplicationContextProvider.getBeanFactory().
                            createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true));
                } catch (ClassNotFoundException e) {
                    LOG.error("Invalid PropagationAction class name '{}' for resource {}", resource, className, e);
                }
            }
        }

        return result;
    }

    /**
     * Transform a {@link Collection} of {@link Attribute} instances into a {@link Map}.
     * The key to each element in the map is the {@code name} of an {@link Attribute}.
     * The value of each element in the map is the {@link Attribute} instance with that name.
     * <br/>
     * Different from the original because:
     * <ul>
     * <li>map keys are transformed toUpperCase()</li>
     * <li>returned map is mutable</li>
     * </ul>
     *
     * @param attributes set of attribute to transform to a map.
     * @return a map of string and attribute.
     *
     * @see org.identityconnectors.framework.common.objects.AttributeUtil#toMap(java.util.Collection)
     */
    private Map<String, Attribute> toMap(final Collection<? extends Attribute> attributes) {
        Map<String, Attribute> map = new HashMap<>();
        for (Attribute attr : attributes) {
            map.put(attr.getName().toUpperCase(), attr);
        }
        return map;
    }

    protected void createOrUpdate(
            final PropagationTask task,
            final ConnectorObject beforeObj,
            final Connector connector,
            final Boolean[] propagationAttempted) {

        // set of attributes to be propagated
        Set<Attribute> attributes = new HashSet<>(task.getAttributes());

        // check if there is any missing or null / empty mandatory attribute
        Set<Object> mandatoryAttrNames = new HashSet<>();
        Attribute mandatoryMissing = AttributeUtil.find(MANDATORY_MISSING_ATTR_NAME, task.getAttributes());
        if (mandatoryMissing != null) {
            attributes.remove(mandatoryMissing);

            if (beforeObj == null) {
                mandatoryAttrNames.addAll(mandatoryMissing.getValue());
            }
        }
        Attribute mandatoryNullOrEmpty = AttributeUtil.find(MANDATORY_NULL_OR_EMPTY_ATTR_NAME, task.getAttributes());
        if (mandatoryNullOrEmpty != null) {
            attributes.remove(mandatoryNullOrEmpty);

            mandatoryAttrNames.addAll(mandatoryNullOrEmpty.getValue());
        }
        if (!mandatoryAttrNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Not attempted because there are mandatory attributes without value(s): " + mandatoryAttrNames);
        }

        if (beforeObj == null) {
            LOG.debug("Create {} on {}", attributes, task.getResource().getKey());
            connector.create(new ObjectClass(task.getObjectClassName()), attributes, null, propagationAttempted);
        } else {
            // 1. check if rename is really required
            Name newName = (Name) AttributeUtil.find(Name.NAME, attributes);

            LOG.debug("Rename required with value {}", newName);

            if (newName != null && newName.equals(beforeObj.getName())
                    && !newName.getNameValue().equals(beforeObj.getUid().getUidValue())) {

                LOG.debug("Remote object name unchanged");
                attributes.remove(newName);
            }

            // 2. check wether anything is actually needing to be propagated, i.e. if there is attribute
            // difference between beforeObj - just read above from the connector - and the values to be propagated
            Map<String, Attribute> originalAttrMap = toMap(beforeObj.getAttributes());
            Map<String, Attribute> updateAttrMap = toMap(attributes);

            // Only compare attribute from beforeObj that are also being updated
            Set<String> skipAttrNames = originalAttrMap.keySet();
            skipAttrNames.removeAll(updateAttrMap.keySet());
            for (String attrName : new HashSet<>(skipAttrNames)) {
                originalAttrMap.remove(attrName);
            }

            Set<Attribute> originalAttrs = new HashSet<>(originalAttrMap.values());

            if (originalAttrs.equals(attributes)) {
                LOG.debug("Don't need to propagate anything: {} is equal to {}", originalAttrs, attributes);
            } else {
                LOG.debug("Attributes that would be updated {}", attributes);

                Set<Attribute> strictlyModified = new HashSet<>();
                for (Attribute attr : attributes) {
                    if (!originalAttrs.contains(attr)) {
                        strictlyModified.add(attr);
                    }
                }

                // 3. provision entry
                LOG.debug("Update {} on {}", strictlyModified, task.getResource().getKey());

                connector.update(
                        beforeObj.getObjectClass(), beforeObj.getUid(), strictlyModified, null, propagationAttempted);
            }
        }
    }

    protected Any<?, ?> getAny(final PropagationTask task) {
        Any<?, ?> any = null;

        if (task.getAnyKey() != null) {
            switch (task.getAnyTypeKind()) {
                case USER:
                    try {
                        any = userDAO.authFind(task.getAnyKey());
                    } catch (Exception e) {
                        LOG.error("Could not read user {}", task.getAnyKey(), e);
                    }
                    break;

                case GROUP:
                    try {
                        any = groupDAO.authFind(task.getAnyKey());
                    } catch (Exception e) {
                        LOG.error("Could not read group {}", task.getAnyKey(), e);
                    }
                    break;

                case ANY_OBJECT:
                default:
                    try {
                        any = anyObjectDAO.authFind(task.getAnyKey());
                    } catch (Exception e) {
                        LOG.error("Could not read any object {}", task.getAnyKey(), e);
                    }
                    break;
            }
        }

        return any;
    }

    protected void delete(
            final PropagationTask task,
            final ConnectorObject beforeObj,
            final Connector connector,
            final Boolean[] propagationAttempted) {

        if (beforeObj == null) {
            LOG.debug("{} not found on external resource: ignoring delete", task.getConnObjectKey());
        } else {
            /*
             * We must choose here whether to
             * a. actually delete the provided any object from the external resource
             * b. just update the provided any object data onto the external resource
             *
             * (a) happens when either there is no any object associated with the PropagationTask (this takes place
             * when the task is generated via UserLogic.delete() / GroupLogic.delete()) or the provided updated
             * any object hasn't the current resource assigned (when the task is generated via
             * UserController.update() / GroupLogic.update()).
             *
             * (b) happens when the provided updated any object does have the current resource assigned (when the task
             * is generated via UserLogic.update() / GroupLogic.updae()): this basically means that before such
             * update, this any object used to have the current resource assigned by more than one mean (for example,
             * two different memberships with the same resource).
             */
            Any<?, ?> any = getAny(task);
            Collection<String> resources = any instanceof User
                    ? userDAO.findAllResourceNames((User) any)
                    : any instanceof AnyObject
                            ? anyObjectDAO.findAllResourceNames((AnyObject) any)
                            : any instanceof Group
                                    ? ((Group) any).getResourceNames()
                                    : Collections.<String>emptySet();
            if (!resources.contains(task.getResource().getKey())) {
                LOG.debug("Delete {} on {}", beforeObj.getUid(), task.getResource().getKey());

                connector.delete(beforeObj.getObjectClass(), beforeObj.getUid(), null, propagationAttempted);
            } else {
                createOrUpdate(task, beforeObj, connector, propagationAttempted);
            }
        }
    }

    @Override
    public TaskExec execute(final PropagationTask task, final PropagationReporter reporter) {
        List<PropagationActions> actions = getPropagationActions(task.getResource());

        Date startDate = new Date();

        TaskExec execution = entityFactory.newEntity(TaskExec.class);
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());

        String taskExecutionMessage = null;
        String failureReason = null;

        // Flag to state whether any propagation has been attempted
        Boolean[] propagationAttempted = new Boolean[] { false };

        ConnectorObject beforeObj = null;
        ConnectorObject afterObj = null;

        Provision provision = null;
        Connector connector = null;
        Result result;
        try {
            provision = task.getResource().getProvision(new ObjectClass(task.getObjectClassName()));
            connector = connFactory.getConnector(task.getResource());

            // Try to read remote object BEFORE any actual operation
            beforeObj = getRemoteObject(task, connector, provision, false);

            for (PropagationActions action : actions) {
                action.before(task, beforeObj);
            }

            switch (task.getOperation()) {
                case CREATE:
                case UPDATE:
                    createOrUpdate(task, beforeObj, connector, propagationAttempted);
                    break;

                case DELETE:
                    delete(task, beforeObj, connector, propagationAttempted);
                    break;

                default:
            }

            execution.setStatus(propagationAttempted[0]
                    ? PropagationTaskExecStatus.SUCCESS.name()
                    : PropagationTaskExecStatus.NOT_ATTEMPTED.name());

            for (PropagationActions action : actions) {
                action.after(task, execution, afterObj);
            }

            LOG.debug("Successfully propagated to {}", task.getResource());
            result = Result.SUCCESS;
        } catch (Exception e) {
            result = Result.FAILURE;
            LOG.error("Exception during provision on resource " + task.getResource().getKey(), e);

            if (e instanceof ConnectorException && e.getCause() != null) {
                taskExecutionMessage = e.getCause().getMessage();
                if (e.getCause().getMessage() == null) {
                    failureReason = e.getMessage();
                } else {
                    failureReason = e.getMessage() + "\n\n Cause: " + e.getCause().getMessage().split("\n")[0];
                }
            } else {
                taskExecutionMessage = ExceptionUtils2.getFullStackTrace(e);
                if (e.getCause() == null) {
                    failureReason = e.getMessage();
                } else {
                    failureReason = e.getMessage() + "\n\n Cause: " + e.getCause().getMessage().split("\n")[0];
                }
            }

            try {
                execution.setStatus(PropagationTaskExecStatus.FAILURE.name());
            } catch (Exception wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted[0] = true;

            for (PropagationActions action : actions) {
                action.onError(task, execution, e);
            }
        } finally {
            // Try to read remote object AFTER any actual operation
            if (connector != null) {
                try {
                    afterObj = getRemoteObject(task, connector, provision, true);
                } catch (Exception ignore) {
                    // ignore exception
                    LOG.error("Error retrieving after object", ignore);
                }
            }

            execution.setStartDate(startDate);
            execution.setMessage(taskExecutionMessage);
            execution.setEndDate(new Date());

            LOG.debug("Execution finished: {}", execution);

            if (hasToBeregistered(task, execution)) {
                LOG.debug("Execution to be stored: {}", execution);

                execution.setTask(task);
                task.addExec(execution);

                taskDAO.save(task);
                // needed to generate a value for the execution key
                taskDAO.flush();
            }

            if (reporter != null) {
                reporter.onSuccessOrNonPriorityResourceFailures(
                        task,
                        PropagationTaskExecStatus.valueOf(execution.getStatus()),
                        failureReason,
                        beforeObj,
                        afterObj);
            }
        }

        notificationManager.createTasks(
                AuditElements.EventCategoryType.PROPAGATION,
                task.getAnyTypeKind().name().toLowerCase(),
                task.getResource().getKey(),
                task.getOperation().name().toLowerCase(),
                result,
                beforeObj, // searching for before object is too much expensive ... 
                new Object[] { execution, afterObj },
                task);

        auditManager.audit(
                AuditElements.EventCategoryType.PROPAGATION,
                task.getAnyTypeKind().name().toLowerCase(),
                task.getResource().getKey(),
                task.getOperation().name().toLowerCase(),
                result,
                beforeObj, // searching for before object is too much expensive ... 
                new Object[] { execution, afterObj },
                task);

        return execution;
    }

    @Override
    public void execute(final Collection<PropagationTask> tasks) {
        execute(tasks, null, false);
    }

    protected abstract void doExecute(
            Collection<PropagationTask> tasks, PropagationReporter reporter, boolean nullPriorityAsync);

    @Override
    public void execute(
            final Collection<PropagationTask> tasks,
            final PropagationReporter reporter,
            final boolean nullPriorityAsync) {

        try {
            doExecute(tasks, reporter, nullPriorityAsync);
        } catch (PropagationException e) {
            LOG.error("Error propagation priority resource", e);
            reporter.onPriorityResourceFailure(e.getResourceName(), tasks);
        }
    }

    /**
     * Check whether an execution has to be stored, for a given task.
     *
     * @param task propagation task
     * @param execution to be decide whether to store or not
     * @return true if execution has to be store, false otherwise
     */
    protected boolean hasToBeregistered(final PropagationTask task, final TaskExec execution) {
        boolean result;

        boolean failed = PropagationTaskExecStatus.valueOf(execution.getStatus()) != PropagationTaskExecStatus.SUCCESS;

        switch (task.getOperation()) {

            case CREATE:
                result = (failed && task.getResource().getCreateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getCreateTraceLevel() == TraceLevel.ALL;
                break;

            case UPDATE:
                result = (failed && task.getResource().getUpdateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getUpdateTraceLevel() == TraceLevel.ALL;
                break;

            case DELETE:
                result = (failed && task.getResource().getDeleteTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getDeleteTraceLevel() == TraceLevel.ALL;
                break;

            default:
                result = false;
        }

        return result;
    }

    /**
     * Get remote object for given task.
     *
     * @param connector connector facade proxy.
     * @param task current propagation task.
     * @param provision provision
     * @param latest 'FALSE' to retrieve object using old connObjectKey if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(
            final PropagationTask task,
            final Connector connector,
            final Provision provision,
            final boolean latest) {

        String connObjectKey = latest || task.getOldConnObjectKey() == null
                ? task.getConnObjectKey()
                : task.getOldConnObjectKey();

        List<MappingItem> linkingMappingItems = new ArrayList<>();
        for (VirSchema schema : virSchemaDAO.findByProvision(provision)) {
            linkingMappingItems.add(schema.asLinkingMappingItem());
        }

        ConnectorObject obj = null;
        try {
            obj = connector.getObject(
                    task.getOperation(),
                    new ObjectClass(task.getObjectClassName()),
                    new Uid(connObjectKey),
                    MappingUtils.buildOperationOptions(IteratorUtils.chainedIterator(
                                    MappingUtils.getPropagationMappingItems(provision).iterator(),
                                    linkingMappingItems.iterator())));

            for (MappingItem item : linkingMappingItems) {
                Attribute attr = obj.getAttributeByName(item.getExtAttrName());
                if (attr == null) {
                    virAttrCache.expire(task.getAnyType(), task.getAnyKey(), item.getIntAttrName());
                } else {
                    VirAttrCacheValue cacheValue = new VirAttrCacheValue();
                    cacheValue.setValues(attr.getValue());
                    virAttrCache.put(task.getAnyType(), task.getAnyKey(), item.getIntAttrName(), cacheValue);
                }
            }
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", connObjectKey, ignore);
        }

        return obj;
    }
}
