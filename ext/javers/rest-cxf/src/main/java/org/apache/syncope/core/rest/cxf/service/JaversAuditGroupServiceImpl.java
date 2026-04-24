package org.apache.syncope.core.rest.cxf.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.JaversAuditGroupService;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAO;

public class JaversAuditGroupServiceImpl extends AbstractService implements JaversAuditGroupService {

    private JaversAuditEventDAO javersAuditEventDAO;

    public JaversAuditGroupServiceImpl(final JaversAuditEventDAO javersAuditEventDAO) {
        this.javersAuditEventDAO = javersAuditEventDAO;
    }

    @Override
    public PagedResult<ShadowTO<GroupTO>> shadows(final String key, final int page, final int size) {
        return buildPagedResult(javersAuditEventDAO.searchForShadows(key, null, null,
                pageable(new AnyQuery.Builder().page(page).size(size).build()), GroupTO.class));
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
                pageable(new AnyQuery.Builder().page(page).size(size).build()), GroupTO.class);
    }
}
