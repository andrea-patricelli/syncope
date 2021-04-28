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
import java.util.concurrent.ExecutionException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;

public abstract class AbstractIndexManager<CE extends ApplicationEvent, DE extends ApplicationEvent>
        implements IndexManager<CE, DE> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractIndexManager.class);

    @Autowired
    protected RestHighLevelClient client;

    @Autowired
    protected ElasticsearchUtils elasticsearchUtils;

    @Override
    public boolean existsIndex(final String domain, final String index) throws IOException {
        return client.indices().exists(
                new GetIndexRequest(ElasticsearchUtils.getContextDomainName(domain, index)), RequestOptions.DEFAULT);
    }

    @Override
    public void createIndex(final String domain, final String index)
            throws InterruptedException, ExecutionException, IOException {

        XContentBuilder settings = XContentFactory.jsonBuilder().
                startObject().
                startObject("analysis").
                startObject("normalizer").
                startObject("string_lowercase").
                field("type", "custom").
                field("char_filter", new Object[0]).
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
                field("normalizer", "string_lowercase").
                endObject().
                endObject().
                endObject().
                endArray().
                endObject();

        CreateIndexResponse response = client.indices().create(
                new CreateIndexRequest(ElasticsearchUtils.getContextDomainName(domain, index)).
                        settings(settings).
                        mapping(mapping), RequestOptions.DEFAULT);
        LOG.debug("Successfully created {} for {}: {}",
                ElasticsearchUtils.getContextDomainName(domain, index), index, response);
    }

    @Override
    public void removeIndex(final String domain, final String index) throws IOException {
        AcknowledgedResponse acknowledgedResponse = client.indices().delete(
                new DeleteIndexRequest(ElasticsearchUtils.getContextDomainName(domain, index)), RequestOptions.DEFAULT);
        LOG.debug("Successfully removed {}: {}",
                ElasticsearchUtils.getContextDomainName(domain, index), acknowledgedResponse);
    }

}
