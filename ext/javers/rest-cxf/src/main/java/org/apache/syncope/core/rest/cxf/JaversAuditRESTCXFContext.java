package org.apache.syncope.core.rest.cxf;

import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.JaversAuditService;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAO;
import org.apache.syncope.core.rest.cxf.service.JaversAuditUserServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class JaversAuditRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public JaversAuditService<UserTO> bpmnProcessService(final JaversAuditEventDAO javersAuditEventDAO) {
        return new JaversAuditUserServiceImpl(javersAuditEventDAO);
    }

}
