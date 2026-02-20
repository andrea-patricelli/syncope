package org.apache.syncope.core.persistence.javers.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JaversAuditEventDAO {

    <T extends AnyTO> Page<ShadowTO<T>> searchForShadows(
            String entityKey,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable,
            Class<T> clazz);

    <T extends AnyTO> List<ChangesByCommitTO> searchForChanges(
            String entityKey,
            String author,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable,
            Class<T> clazz);

}
