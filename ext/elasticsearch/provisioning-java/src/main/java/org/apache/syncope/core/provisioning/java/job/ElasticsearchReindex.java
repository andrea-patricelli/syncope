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
package org.apache.syncope.core.provisioning.java.job;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove and rebuild all Elasticsearch indexes with information from existing users, groups and any objects.
 */
public class ElasticsearchReindex extends AbstractSchedTaskJobDelegate {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ElasticsearchIndexManager indexManager;

    @Autowired
    private ElasticsearchUtils elasticsearchUtils;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Override
    protected String doExecute(final boolean dryRun, final String executor) throws JobExecutionException {
        if (!dryRun) {
            LOG.debug("Start rebuilding indexes");

            try {
                removeIndexIfExists(AnyTypeKind.USER.name());
                removeIndexIfExists(AnyTypeKind.GROUP.name());
                removeIndexIfExists(AnyTypeKind.ANY_OBJECT.name());
                removeIndexIfExists(Task.class.getSimpleName());

                createIndex(AnyTypeKind.USER.name());
                createIndex(AnyTypeKind.GROUP.name());
                createIndex(AnyTypeKind.ANY_OBJECT.name());

                LOG.debug("Indexing users...");
                for (int page = 1; page <= (userDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (String user : userDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(AnyTypeKind.USER.name()),
                                AnyTypeKind.USER.name(),
                                user).
                                source(elasticsearchUtils.builder(userDAO.find(user)));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", user, response);
                    }
                }

                LOG.debug("Indexing groups...");
                for (int page = 1; page <= (groupDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (String group : groupDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(AnyTypeKind.GROUP.name()),
                                AnyTypeKind.GROUP.name(),
                                group).
                                source(elasticsearchUtils.builder(groupDAO.find(group)));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", group, response);
                    }
                }

                LOG.debug("Indexing any objects...");
                for (int page = 1; page <= (anyObjectDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (String anyObject : anyObjectDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(AnyTypeKind.ANY_OBJECT.name()),
                                AnyTypeKind.ANY_OBJECT.name(),
                                anyObject).
                                source(elasticsearchUtils.builder(anyObjectDAO.find(anyObject)));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", anyObject, response);
                    }
                }

                LOG.debug("Indexing Propagation Tasks...");
                for (int page = 1; page <= (taskDAO.count(PropagationTask.class, null, null, null, null)
                        / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (PropagationTask propTask : taskDAO.findAll(
                            PropagationTask.class, null, null, null, null, page, AnyDAO.DEFAULT_PAGE_SIZE,
                            Collections.<OrderByClause>emptyList())) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(Task.class.getSimpleName()),
                                PropagationTask.class.getSimpleName(),
                                propTask.getKey()).
                                source(elasticsearchUtils.builder(propTask));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", propTask, response);
                    }
                }

                LOG.debug("Rebuild indexes for domain {} successfully completed", AuthContextUtils.getDomain());
            } catch (Exception e) {
                throw new JobExecutionException("While rebuilding index for domain " + AuthContextUtils.getDomain(), e);
            }
        }

        return "SUCCESS";
    }

    @SuppressWarnings("deprecation")
    private void removeIndexIfExists(final String index) throws IOException {
        if (client.indices().exists(
                new GetIndexRequest().indices(elasticsearchUtils.getContextDomainName(index)),
                RequestOptions.DEFAULT)) {

            AcknowledgedResponse acknowledgedResponse = client.indices().delete(
                    new DeleteIndexRequest(elasticsearchUtils.getContextDomainName(index)), RequestOptions.DEFAULT);
            LOG.debug("Successfully removed {}: {}",
                    elasticsearchUtils.getContextDomainName(index), acknowledgedResponse);
        }
    }

    @SuppressWarnings("deprecation")
    private void createIndex(final String index)
            throws InterruptedException, ExecutionException, IOException {

        XContentBuilder settings = XContentFactory.jsonBuilder().
                startObject().
                startObject("analysis").
                startObject("analyzer").
                startObject("string_lowercase").
                field("type", "custom").
                field("tokenizer", "standard").
                field("filter").
                startArray().
                value("lowercase").
                endArray().
                endObject().
                endObject().
                endObject().
                startObject("index").
                field("number_of_shards", elasticsearchUtils.getNumberOfShards()).
                field("number_of_replicas", elasticsearchUtils.getNumberOfReplicas()).
                endObject().
                endObject();

        XContentBuilder mapping = XContentFactory.jsonBuilder().
                startObject().
                startArray("dynamic_templates").
                startObject().
                startObject("strings").
                field("match_mapping_type", "string").
                startObject("mapping").
                field("type", "keyword").
                field("analyzer", "string_lowercase").
                endObject().
                endObject().
                endObject().
                endArray().
                endObject();

        CreateIndexResponse response = client.indices().create(
                new CreateIndexRequest(elasticsearchUtils.getContextDomainName(index)).
                        settings(settings).
                        mapping(index, mapping), RequestOptions.DEFAULT);
        LOG.debug("Successfully created {} for {}: {}",
                elasticsearchUtils.getContextDomainName(index), index, response);
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }
}
