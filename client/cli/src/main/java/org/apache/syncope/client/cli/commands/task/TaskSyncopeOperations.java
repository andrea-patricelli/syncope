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
package org.apache.syncope.client.cli.commands.task;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;

public class TaskSyncopeOperations {

    private final TaskService taskService = SyncopeServices.get(TaskService.class);

    public List<TaskExecTO> listScheduledJobs() {
        return taskService.listJobs(JobStatusType.SCHEDULED);
    }

    public List<TaskExecTO> listRunningJobs() {
        return taskService.listJobs(JobStatusType.RUNNING);
    }

    public <T extends AbstractTaskTO> T read(final String taskId) {
        return taskService.read(Long.valueOf(taskId));
    }

    public void delete(final String taskId) {
        taskService.read(Long.valueOf(taskId));
    }

    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType type, final TaskQuery query) {
        return taskService.list(type, query);
    }

    public TaskExecTO readExecution(final String executionId) {
        return taskService.readExecution(Long.valueOf(executionId));
    }

    public void deleteExecution(final String executionId) {
        taskService.deleteExecution(Long.valueOf(executionId));
    }

    public TaskExecTO execute(final String executionId, final boolean dryRun) {
        return taskService.execute(Long.valueOf(executionId), dryRun);
    }
}
