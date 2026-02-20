package org.apache.syncope.ext.javers.client;

import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.javers.client.util.JaversDomainLocator;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.javers.core.Javers;
import org.javers.repository.jql.InstanceIdDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

public class JaversAuditManager {

    private static final Logger LOG = LoggerFactory.getLogger(JaversAuditManager.class);

    protected final JaversDomainLocator javersDomainLocator;

    private final UserDataBinder userDataBinder;

    private final GroupDataBinder groupDataBinder;

    private final AnyObjectDataBinder anyObjectDataBinder;

    public JaversAuditManager(
            final JaversDomainLocator javersDomainLocator,
            final UserDataBinder userDataBinder,
            final GroupDataBinder groupDataBinder,
            final AnyObjectDataBinder anyObjectDataBinder) {
        this.javersDomainLocator = javersDomainLocator;
        this.userDataBinder = userDataBinder;
        this.groupDataBinder = groupDataBinder;
        this.anyObjectDataBinder = anyObjectDataBinder;
    }

    @TransactionalEventListener
    public void entity(final EntityLifecycleEvent<? extends Entity> event) {
        Javers javers = javersDomainLocator.getBean(AuthContextUtils.getDomain(), Javers.class);

        LOG.info("About to audit {} for {}", event.getType().name(), event.getEntity());

        if (event.getEntity() instanceof User) {
            if (event.getType() == SyncDeltaType.CREATE_OR_UPDATE || event.getType() == SyncDeltaType.CREATE
                    || event.getType() == SyncDeltaType.UPDATE) {
                UserTO userTO = userDataBinder.getUserTO((User) event.getEntity(), true);
                LOG.debug("About to commit CREATE_OR_UPDATE event on user {} with additional infos {}",
                        userTO.getUsername(), event.getAdditionalInfos());
                javers.commit(AuthContextUtils.getUsername(), userTO, event.getAdditionalInfos());
            } else if (event.getType() == SyncDeltaType.DELETE) {
                LOG.debug("About to commit DELETE event on user {} with additional infos {}",
                        ((User) event.getEntity()).getUsername(), event.getAdditionalInfos());
                javers.commitShallowDeleteById(AuthContextUtils.getUsername(),
                        InstanceIdDTO.instanceId(event.getEntity().getKey(), UserTO.class), event.getAdditionalInfos());
            }
        }
    }
}
