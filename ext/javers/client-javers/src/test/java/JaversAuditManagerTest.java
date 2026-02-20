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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.apache.syncope.ext.javers.client.JaversAuditManager;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JaversAuditManagerTest extends AbstractTest {

    @AfterAll
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private Javers javers;

    @Autowired
    private JaversAuditManager javersAuditManager;

    @Test
    public void userCRUD() {
        // 1. create user
        User user01 = entityFactory.newEntity(User.class);
        user01.setUsername("test.javers" + RandomStringUtils.secure().nextNumeric(4) + "@syncope.apache.org");
        user01.setRealm(realmDAO.getRoot());
        user01.setCreator("admin");
        user01.setCreationDate(OffsetDateTime.now());
        user01.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user01.setPassword("password123");

        // 2.1 assign group to user
        UMembership uMembershipStr01 = entityFactory.newEntity(UMembership.class);
        uMembershipStr01.setLeftEnd(user01);
        uMembershipStr01.setRightEnd(groupDAO.findByName("employee").orElseThrow());
        user01.add(uMembershipStr01);

        user01 = userDAO.save(user01);
        assertNotNull(user01);

        User user02 = entityFactory.newEntity(User.class);
        user02.setUsername("test.javers" + RandomStringUtils.secure().nextNumeric(4) + "@syncope.apache.org");
        user02.setRealm(realmDAO.getRoot());
        user02.setCreator("admin");
        user02.setCreationDate(OffsetDateTime.now());
        user02.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user02.setPassword("password123");

        // 2.1 assign group to user02
        UMembership uMembershipStr02 = entityFactory.newEntity(UMembership.class);
        uMembershipStr02.setLeftEnd(user02);
        uMembershipStr02.setRightEnd(groupDAO.findByName("employee").orElseThrow());
        user02.add(uMembershipStr02);

        user02 = userDAO.save(user02);
        assertNotNull(user02);

        entityManager.flush();

        // evento su user01
        javersAuditManager.entity(
                new EntityLifecycleEvent<>(Mockito.mock(UserWorkflowAdapter.class), SyncDeltaType.CREATE_OR_UPDATE,
                        user01, SyncopeConstants.MASTER_DOMAIN).addAdditionalInfo("context", "somecontext")
                        .addAdditionalInfo("category", "somecategory")
                        .addAdditionalInfo("subcategory", "somesubcategory"));
        // evento su user02
        javersAuditManager.entity(
                new EntityLifecycleEvent<>(Mockito.mock(UserWorkflowAdapter.class), SyncDeltaType.CREATE_OR_UPDATE,
                        user02, SyncopeConstants.MASTER_DOMAIN));

        // evento DELETE su user02
        javersAuditManager.entity(
                new EntityLifecycleEvent<>(Mockito.mock(UserWorkflowAdapter.class), SyncDeltaType.DELETE, user02,
                        SyncopeConstants.MASTER_DOMAIN));

        Changes changesUser01 = javers.findChanges(QueryBuilder.byInstanceId(user01.getKey(), UserTO.class).build());
        assertFalse(changesUser01.groupByCommit().isEmpty());
        assertEquals(1, changesUser01.groupByCommit().size());

        List<Shadow<UserTO>> shadowsUser01 =
                javers.findShadows(QueryBuilder.byInstanceId(user01.getKey(), UserTO.class).build());
        assertFalse(shadowsUser01.isEmpty());
        assertEquals("somecontext", shadowsUser01.getFirst().getCommitMetadata().getProperties().get("context"));
        assertEquals("somecategory", shadowsUser01.getFirst().getCommitMetadata().getProperties().get("category"));
        assertEquals("somesubcategory",
                shadowsUser01.getFirst().getCommitMetadata().getProperties().get("subcategory"));

        Changes changesUser02 = javers.findChanges(QueryBuilder.byInstanceId(user02.getKey(), UserTO.class).build());
        assertFalse(changesUser02.groupByObject().isEmpty());
        assertEquals(2, changesUser02.groupByCommit().size());
    }

}
