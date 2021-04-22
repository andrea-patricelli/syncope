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
package org.apache.syncope.ext.elasticsearch.client;

import java.io.IOException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listen to any create / update and delete in order to keep the Elasticsearch indexes consistent.
 */
public class ElasticsearchAnyIndexManager 
        extends AbstractIndexManager<AnyCreatedUpdatedEvent<Any<?>>, AnyDeletedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchAnyIndexManager.class);

    @TransactionalEventListener
    @Override
    public void afterCreate(final AnyCreatedUpdatedEvent<Any<?>> event) throws IOException {
        GetRequest getRequest = new GetRequest(
                ElasticsearchUtils.getContextDomainName(
                        AuthContextUtils.getDomain(), event.getAny().getType().getKind()),
                event.getAny().getKey());
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            LOG.debug("About to update index for {}", event.getAny());

            UpdateRequest request = new UpdateRequest(
                    ElasticsearchUtils.getContextDomainName(
                            AuthContextUtils.getDomain(), event.getAny().getType().getKind()),
                    event.getAny().getKey()).
                    retryOnConflict(elasticsearchUtils.getRetryOnConflict()).
                    doc(elasticsearchUtils.builder(event.getAny()));
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully updated for {}: {}", event.getAny(), response);
        } else {
            LOG.debug("About to create index for {}", event.getAny());

            IndexRequest request = new IndexRequest(
                    ElasticsearchUtils.getContextDomainName(
                            AuthContextUtils.getDomain(), event.getAny().getType().getKind())).
                    id(event.getAny().getKey()).
                    source(elasticsearchUtils.builder(event.getAny()));
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully created for {}: {}", event.getAny(), response);
        }
    }

    @TransactionalEventListener
    @Override
    public void afterDelete(final AnyDeletedEvent event) throws IOException {
        LOG.debug("About to delete index for {}[{}]", event.getAnyTypeKind(), event.getAnyKey());

        DeleteRequest request = new DeleteRequest(
                ElasticsearchUtils.getContextDomainName(AuthContextUtils.getDomain(), event.getAnyTypeKind()),
                event.getAnyKey());
        DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        LOG.debug("Index successfully deleted for {}[{}]: {}",
                event.getAnyTypeKind(), event.getAnyKey(), response);
    }

}
