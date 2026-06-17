package org.apache.syncope.core.rest.cxf.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.JaversAuditAnyObjectService;
import org.apache.syncope.core.persistence.javers.dao.JaversAuditEventDAO;

public class JaversAuditAnyObjectServiceImpl extends AbstractService implements JaversAuditAnyObjectService {

    private final JaversAuditEventDAO javersAuditEventDAO;

    public JaversAuditAnyObjectServiceImpl(final JaversAuditEventDAO javersAuditEventDAO) {
        this.javersAuditEventDAO = javersAuditEventDAO;
    }

    @Override
    public PagedResult<ShadowTO<AnyObjectTO>> shadows(final String key, final int page, final int size) {
        return buildPagedResult(javersAuditEventDAO.searchForShadows(key, null, null,
                pageable(new AnyQuery.Builder().page(page).size(size).build()), AnyObjectTO.class));
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
                pageable(new AnyQuery.Builder().page(page).size(size).build()), AnyObjectTO.class);
    }
}
