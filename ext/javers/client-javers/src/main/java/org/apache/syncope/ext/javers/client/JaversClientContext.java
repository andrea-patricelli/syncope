package org.apache.syncope.ext.javers.client;

import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.ext.javers.client.util.JaversDomainLocator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(JaversClientProperties.class)
@AutoConfiguration
public class JaversClientContext {

    @ConditionalOnMissingBean
    @Bean
    public JaversAuditManager javersAuditManager(
            final JaversDomainLocator javersDomainLocator,
            final UserDataBinder userDataBinder,
            final GroupDataBinder groupDataBinder,
            final AnyObjectDataBinder anyObjectDataBinder) {
        return new JaversAuditManager(javersDomainLocator, userDataBinder, groupDataBinder, anyObjectDataBinder);
    }

}
