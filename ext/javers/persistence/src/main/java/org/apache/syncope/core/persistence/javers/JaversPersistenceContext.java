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
package org.apache.syncope.core.persistence.javers;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAO;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAOImpl;
import org.apache.syncope.core.persistence.javers.loader.JaversStartupDomainLoader;
import org.apache.syncope.ext.javers.client.util.JaversDomainLocator;
import org.javers.repository.sql.ConnectionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@EnableConfigurationProperties(JaversPersistenceProperties.class)
@Configuration(proxyBeanMethods = false)
public class JaversPersistenceContext {

    @Bean
    public JaversDomainLocator javersDomainLocator(final ConfigurableApplicationContext ctx) {
        return new JaversDomainLocator(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnectionProvider connectionProvider(final EntityManager entityManager) {
        return new JaversConnectionProvider(entityManager);
    }

    @ConditionalOnMissingBean(name = "javersAuditEventDAO")
    @Bean
    public JaversAuditEventDAO javersAuditEventDAO(final JaversDomainLocator javersDomainLocator) {
        return new JaversAuditEventDAOImpl(javersDomainLocator);
    }

    @ConditionalOnMissingBean
    @Bean
    public JaversStartupDomainLoader javersStartupDomainLoader(
            final ConfigurableApplicationContext ctx,
            final JaversPersistenceProperties props,
            final DomainOps domainOps,
            final DomainHolder<DataSource> domainHolder,
            final PlatformTransactionManager platformTransactionManager,
            final ConnectionProvider connectionProvider) {
        return new JaversStartupDomainLoader(ctx, domainOps, domainHolder, platformTransactionManager,
                connectionProvider, props);
    }

}
