package org.apache.syncope.core.rest.cxf.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.JaversAuditUserService;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAO;

public class JaversAuditUserServiceImpl extends AbstractService implements JaversAuditUserService {

    private JaversAuditEventDAO javersAuditEventDAO;

    public JaversAuditUserServiceImpl(final JaversAuditEventDAO javersAuditEventDAO) {
        this.javersAuditEventDAO = javersAuditEventDAO;
    }

    @Override
    public PagedResult<ShadowTO<UserTO>> shadows(final String key, final int page, final int size) {
        return buildPagedResult(javersAuditEventDAO.searchForShadows(key, null, null,
                pageable(new AnyQuery.Builder().page(page).size(size).build()), UserTO.class));
    }

    @Override
    public List<ChangesByCommitTO> changes(
            final String key,
            final String author,
            final OffsetDateTime from,
            final OffsetDateTime to,
            final int page,
            final int size) {
        return javersAuditEventDAO.searchForChanges(key, author, from, to,
                pageable(new AnyQuery.Builder().page(page).size(size).build()), UserTO.class);
    }
}
