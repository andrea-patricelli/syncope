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
package org.apache.syncope.core.rest.cxf.service;

import java.net.URI;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.logic.TaskLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends AbstractServiceImpl implements TaskService {

    @Autowired
    private TaskLogic logic;

    @Override
    public Response create(final SchedTaskTO taskTO) {
        SchedTaskTO createdTask;
        if (taskTO instanceof SyncTaskTO || taskTO instanceof PushTaskTO || taskTO instanceof SchedTaskTO) {
            createdTask = logic.createSchedTask(taskTO);
        } else {
            throw new BadRequestException();
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdTask.getKey())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, createdTask.getKey()).
                build();
    }

    @Override
    public void delete(final Long key) {
        logic.delete(key);
    }

    @Override
    public void deleteExecution(final Long executionKey) {
        logic.deleteExecution(executionKey);
    }

    @Override
    public TaskExecTO execute(final Long key, final boolean dryRun) {
        return logic.execute(key, dryRun);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType type, final TaskQuery query) {
        return (PagedResult<T>) buildPagedResult(
                logic.list(
                        type,
                        query.getResource(),
                        query.getAnyTypeKind(),
                        query.getAnyTypeKey(),
                        query.getPage(),
                        query.getSize(),
                        getOrderByClauses(query.getOrderBy())),
                query.getPage(),
                query.getSize(),
                logic.count(
                        type,
                        query.getResource(),
                        query.getAnyTypeKind(),
                        query.getAnyTypeKey()));
    }

    @Override
    public <T extends AbstractTaskTO> T read(final Long key) {
        return logic.read(key);
    }

    @Override
    public TaskExecTO readExecution(final Long executionKey) {
        return logic.readExecution(executionKey);
    }

    @Override
    public void update(final AbstractTaskTO taskTO) {
        if (taskTO instanceof SyncTaskTO) {
            logic.updateSync((SyncTaskTO) taskTO);
        } else if (taskTO instanceof SchedTaskTO) {
            logic.updateSched((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult result = new BulkActionResult();

        switch (bulkAction.getType()) {
            case DELETE:
                for (String key : bulkAction.getTargets()) {
                    try {
                        result.getResults().put(
                                String.valueOf(logic.delete(Long.valueOf(key)).getKey()),
                                BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for task {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            case DRYRUN:
                for (String key : bulkAction.getTargets()) {
                    try {
                        logic.execute(Long.valueOf(key), true);
                        result.getResults().put(key, BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing dryrun for task {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            case EXECUTE:
                for (String key : bulkAction.getTargets()) {
                    try {
                        logic.execute(Long.valueOf(key), false);
                        result.getResults().put(key, BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing execute for task {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            default:
        }

        return result;
    }

    @Override
    public List<TaskExecTO> listJobs(final JobStatusType type) {
        return logic.listJobs(type, TaskExecTO.class);
    }

    @Override
    public void actionJob(final Long key, final JobAction action) {
        logic.actionJob(key, action);
    }
}
