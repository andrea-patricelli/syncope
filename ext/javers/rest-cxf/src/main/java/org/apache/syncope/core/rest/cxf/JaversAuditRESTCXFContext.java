package org.apache.syncope.core.rest.cxf;

import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.JaversAuditService;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAO;
import org.apache.syncope.core.rest.cxf.service.JaversAuditAnyObjectServiceImpl;
import org.apache.syncope.core.rest.cxf.service.JaversAuditGroupServiceImpl;
import org.apache.syncope.core.rest.cxf.service.JaversAuditUserServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class JaversAuditRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public JaversAuditService<UserTO> javersAuditUserService(final JaversAuditEventDAO javersAuditEventDAO) {
        return new JaversAuditUserServiceImpl(javersAuditEventDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public JaversAuditService<GroupTO> javersAuditGroupService(final JaversAuditEventDAO javersAuditEventDAO) {
        return new JaversAuditGroupServiceImpl(javersAuditEventDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public JaversAuditService<AnyObjectTO> javersAuditAnyObjectService(final JaversAuditEventDAO javersAuditEventDAO) {
        return new JaversAuditAnyObjectServiceImpl(javersAuditEventDAO);
    }

}
