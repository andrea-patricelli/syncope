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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.AbstractTask;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

@Repository
public class JPATaskDAO extends AbstractDAO<Task, Long> implements TaskDAO {

    @Override
    public Class<? extends Task> getEntityReference(final TaskType type) {
        Class<? extends Task> result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTask.class;
                break;

            case PROPAGATION:
                result = JPAPropagationTask.class;
                break;

            case PUSH:
                result = JPAPushTask.class;
                break;

            case SCHEDULED:
                result = JPASchedTask.class;
                break;

            case SYNCHRONIZATION:
                result = JPASyncTask.class;
                break;

            default:
        }

        return result;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task> T find(final Long key) {
        return (T) entityManager().find(AbstractTask.class, key);
    }

    private <T extends Task> StringBuilder buildFindAllQuery(final TaskType type) {
        return new StringBuilder("SELECT t FROM ").
                append(getEntityReference(type).getSimpleName()).
                append(" t WHERE t.type=:type ");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> List<T> findToExec(final TaskType type) {
        StringBuilder queryString = buildFindAllQuery(type).append("AND ");

        if (type == TaskType.NOTIFICATION) {
            queryString.append("t.executed = 0 ");
        } else {
            queryString.append("t.executions IS EMPTY ");
        }
        queryString.append("ORDER BY t.id DESC");

        Query query = entityManager().createQuery(queryString.toString());
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends Task> List<T> findAll(final TaskType type) {
        return findAll(type, null, null, null, -1, -1, Collections.<OrderByClause>emptyList());
    }

    private StringBuilder buildFindAllQuery(
            final TaskType type,
            final ExternalResource resource,
            final AnyTypeKind anyTypeKind,
            final Long anyTypeKey) {

        if (resource != null
                && type != TaskType.PROPAGATION && type != TaskType.PUSH && type != TaskType.SYNCHRONIZATION) {

            throw new IllegalArgumentException(type + " is not related to " + ExternalResource.class.getSimpleName());
        }

        if ((anyTypeKind != null || anyTypeKey != null) && type != TaskType.PROPAGATION) {
            throw new IllegalArgumentException(type + " is not related to users, groups or any objects");
        }

        StringBuilder queryString = buildFindAllQuery(type);

        if (resource != null) {
            queryString.append("AND t.resource=:resource ");
        }
        if (anyTypeKind != null && anyTypeKey != null) {
            queryString.append("AND t.anyTypeKind=:anyTypeKind AND t.anyTypeKey=:anyTypeKey ");
        }

        return queryString;
    }

    private String toOrderByStatement(
            final Class<? extends Task> beanClass, final List<OrderByClause> orderByClauses) {

        StringBuilder statement = new StringBuilder();

        for (OrderByClause clause : orderByClauses) {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(beanClass, field) != null) {
                statement.append("t.").append(field).append(' ').append(clause.getDirection().name());
            }
        }

        if (statement.length() > 0) {
            statement.insert(0, "ORDER BY ");
        }
        return statement.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> List<T> findAll(
            final TaskType type,
            final ExternalResource resource,
            final AnyTypeKind anyTypeKind,
            final Long anyTypeKey,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderByClauses) {

        StringBuilder queryString = buildFindAllQuery(type, resource, anyTypeKind, anyTypeKey).
                append(orderByClauses.isEmpty()
                                ? "ORDER BY t.id DESC"
                                : toOrderByStatement(getEntityReference(type), orderByClauses));

        Query query = entityManager().createQuery(queryString.toString());
        query.setParameter("type", type);
        if (resource != null) {
            query.setParameter("resource", resource);
        }
        if (anyTypeKind != null && anyTypeKey != null) {
            query.setParameter("anyTypeKind", anyTypeKind);
            query.setParameter("anyTypeKey", anyTypeKey);
        }

        query.setFirstResult(itemsPerPage * (page <= 0
                ? 0
                : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public int count(
            final TaskType type,
            final ExternalResource resource,
            final AnyTypeKind anyTypeKind,
            final Long anyTypeKey) {

        StringBuilder queryString = buildFindAllQuery(type, resource, anyTypeKind, anyTypeKey);

        Query query = entityManager().createQuery(queryString.toString().replace(
                "SELECT t",
                "SELECT COUNT(t)"));
        query.setParameter("type", type);
        if (resource != null) {
            query.setParameter("resource", resource);
        }
        if (anyTypeKind != null && anyTypeKey != null) {
            query.setParameter("anyTypeKind", anyTypeKind);
            query.setParameter("anyTypeKey", anyTypeKey);
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task> T save(final T task) {
        return entityManager().merge(task);
    }

    @Override
    public void delete(final Long id) {
        Task task = find(id);
        if (task == null) {
            return;
        }

        delete(task);
    }

    @Override
    public void delete(final Task task) {
        entityManager().remove(task);
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        CollectionUtils.forAllDo(
                findAll(type, resource, null, null, -1, -1, Collections.<OrderByClause>emptyList()),
                new Closure<Task>() {

                    @Override
                    public void execute(final Task input) {
                        delete(input.getKey());
                    }
                });
    }
}
