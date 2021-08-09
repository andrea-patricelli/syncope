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
package org.apache.syncope.core.persistence.jpa;

import javax.sql.DataSource;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class DomainIndexLoader implements SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(DomainIndexLoader.class);

    @Autowired
    @Qualifier("anyIndexManager")
    private IndexManager anyIndexManager;

    @Autowired
    @Qualifier("taskIndexManager")
    private IndexManager taskIndexManager;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        try {
            if (!anyIndexManager.existsIndex(domain, AnyTypeKind.USER.name())) {
                anyIndexManager.createIndex(domain, AnyTypeKind.USER.name());
            }
            if (!anyIndexManager.existsIndex(domain, AnyTypeKind.GROUP.name())) {
                anyIndexManager.createIndex(domain, AnyTypeKind.GROUP.name());
            }
            if (!anyIndexManager.existsIndex(domain, AnyTypeKind.ANY_OBJECT.name())) {
                anyIndexManager.createIndex(domain, AnyTypeKind.ANY_OBJECT.name());
            }
            if (taskIndexManager.existsIndex(AuthContextUtils.getDomain(), Task.class.getSimpleName())) {
                taskIndexManager.removeIndex(AuthContextUtils.getDomain(), Task.class.getSimpleName());
            }
            taskIndexManager.createIndex(AuthContextUtils.getDomain(), Task.class.getSimpleName());
        } catch (Exception e) {
            LOG.error("While creating index for domain {}", domain, e);
        }
    }
}
