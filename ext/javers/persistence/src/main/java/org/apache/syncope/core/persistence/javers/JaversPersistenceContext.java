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
