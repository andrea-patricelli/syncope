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
package org.apache.syncope.core.persistence.javers.loader;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.javers.JaversPersistenceProperties;
import org.apache.syncope.core.persistence.javers.OpenJpaUnproxyObjectAccessHook;
import org.javers.core.Javers;
import org.javers.core.metamodel.clazz.EntityDefinitionBuilder;
import org.javers.repository.sql.ConnectionProvider;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.javers.spring.jpa.TransactionalJpaJaversBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;

public class JaversStartupDomainLoader implements SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(JaversStartupDomainLoader.class);

    protected final ConfigurableApplicationContext ctx;

    protected final DomainOps domainOps;

    protected final DomainHolder<DataSource> domainHolder;

    protected final JaversPersistenceProperties persistenceProperties;

    protected final PlatformTransactionManager domainTransactionManager;

    protected final ConnectionProvider connectionProvider;

    public JaversStartupDomainLoader(
            final ConfigurableApplicationContext ctx,
            final DomainOps domainOps,
            final DomainHolder<DataSource> domainHolder,
            final PlatformTransactionManager domainTransactionManager,
            final ConnectionProvider connectionProvider,
            final JaversPersistenceProperties persistenceProperties) {

        this.ctx = ctx;
        this.domainOps = domainOps;
        this.domainHolder = domainHolder;
        this.persistenceProperties = persistenceProperties;
        this.domainTransactionManager = domainTransactionManager;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void load() {
        Map<String, JPADomain> keymasterDomains =
                domainOps.list().stream().collect(Collectors.toMap(Domain::getKey, JPADomain.class::cast));

        persistenceProperties.getDomain()
                .stream()
                .filter(d -> !domainHolder.getDomains().containsKey(d.getKey()))
                .forEach(domainProps -> {
                    if (keymasterDomains.containsKey(domainProps.getKey())) {
                        LOG.info("Javers initialization for domain {}", domainProps.getKey());
                        buildAndRegisterJavers(domainProps.getKey(), domainProps.getDatabaseDialect());
                    } else {
                        LOG.warn("Domain {} not found in KeyMaster, skipping Javers initialization",
                                domainProps.getKey());
                    }
                });
    }

    @Override
    public void load(final String domain) {
        if (ctx.containsBean("javers" + StringUtils.capitalize(domain))) {
            LOG.info("Javers for domain {} already initialized, skipping loading", domain);
        } else {
            buildAndRegisterJavers(domain, persistenceProperties.getDomain()
                    .stream()
                    .filter(dp -> domain.equalsIgnoreCase(dp.getKey()))
                    .findFirst()
                    .orElseThrow()
                    .getDatabaseDialect());
        }
    }

    protected void buildAndRegisterJavers(final String domain, final String dialect) {
        String javersBeanName = "javers" + StringUtils.capitalize(domain);
        LOG.info("Javers initialization for domain {}, registering singleton bean {}", domain, javersBeanName);

        beanFactory().registerBeanDefinition(javersBeanName, BeanDefinitionBuilder.rootBeanDefinition(Javers.class,
                () -> TransactionalJpaJaversBuilder.javers()
                        .withTxManager(domainTransactionManager)
                        .withObjectAccessHook(new OpenJpaUnproxyObjectAccessHook())
                        .registerJaversRepository(SqlRepositoryBuilder.sqlRepository()
                                .withConnectionProvider(connectionProvider)
                                .withDialect(DialectName.valueOf(dialect))
                                .build())
                        .registerEntity(EntityDefinitionBuilder.entityDefinition(UserTO.class)
                                .withIdPropertyName("key")
                                .withIgnoredProperties(
                                        List.of("token", "password", "tokenExpireTime", "securityAnswer"))
                                .build())
                        .registerIgnoredClassesStrategy(c -> c.getName().startsWith("org.apache.syncope"))
                        .build()).getBeanDefinition());
        LOG.info("Javers for domain {} successfully inited. Bean definition for [{}] created", domain, javersBeanName);
    }

    protected DefaultListableBeanFactory beanFactory() {
        return (DefaultListableBeanFactory) ctx.getBeanFactory();
    }

}
