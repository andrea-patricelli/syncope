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
package org.apache.syncope.core.persistence.javers.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.ext.javers.client.util.JaversDomainLocator;
import org.javers.core.Changes;
import org.javers.core.ChangesByCommit;
import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.PropertyChangeType;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.CollectionChange;
import org.javers.core.diff.changetype.container.SetChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.SnapshotType;
import org.javers.repository.jql.JqlQuery;
import org.javers.shadow.Shadow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
public class JaversAuditEventDAOTest {

    @Mock
    private JaversDomainLocator javersDomainLocator;

    @Mock
    private Javers javers;

    private JaversAuditEventDAO auditEventDAO;

    @BeforeEach
    protected void setupSearchDAO() {
        when(javersDomainLocator.getBean(SyncopeConstants.MASTER_DOMAIN, Javers.class)).thenReturn(javers);

        auditEventDAO = new JaversAuditEventDAOImpl(javersDomainLocator);
    }

    @Test
    public void searchForUserSnapshots() {
        UserTO user01 = new UserTO();
        user01.setKey(UUID.randomUUID().toString());
        user01.setUsername("testuser");

        List<Shadow<UserTO>> shadows0To100 = generateUserShadows(user01, 0, 100);
        List<Shadow<UserTO>> shadows100To200 = generateUserShadows(user01, 100, 190);

        when(javers.findShadowsAndStream(ArgumentMatchers.any(JqlQuery.class))).thenAnswer(ic -> shadows0To100.stream())
                .thenAnswer(ic -> shadows100To200.stream());

        // first page
        Page<ShadowTO<UserTO>> events0To100 =
                auditEventDAO.searchForShadows(user01.getKey(), null, null, PageRequest.of(0, 100), UserTO.class);
        assertEquals(100, events0To100.getTotalElements());
        assertTrue(events0To100.getContent()
                .getFirst()
                .getWhen()
                .truncatedTo(ChronoUnit.HOURS)
                .isEqual(OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS)));
        assertEquals("admin_of_testuser_v0", events0To100.getContent().getFirst().getWho());
        assertEquals("testuser_v0", events0To100.getContent().getFirst().getAnyTO().getUsername());
        assertEquals("testuser_v11", events0To100.getContent().get(11).getAnyTO().getUsername());
        // second page
        Page<ShadowTO<UserTO>> events100To200 =
                auditEventDAO.searchForShadows(user01.getKey(), null, null, PageRequest.of(1, 100), UserTO.class);
        assertEquals(90, events100To200.getTotalElements());
        assertEquals("testuser_v185", events100To200.getContent().get(85).getAnyTO().getUsername());
    }

    @Test
    public void searchForUserChanges() {
        UserTO user01 = new UserTO();
        user01.setKey(UUID.randomUUID().toString());
        user01.setUsername("testuser");

        Changes changesUser01 = generateUserChanges(user01);

        when(javers.findChanges(ArgumentMatchers.any(JqlQuery.class))).thenAnswer(ic -> changesUser01);

        // first page
        List<ChangesByCommitTO> changes =
                auditEventDAO.searchForChanges(user01.getKey(), "admin", null, null, PageRequest.of(0, 100),
                        UserTO.class);
        assertNotNull(changes);
        assertEquals(5, changes.getFirst().getChanges().getValueChanges().size());
        assertTrue(changes.getFirst()
                .getChanges()
                .getWhen()
                .truncatedTo(ChronoUnit.HOURS)
                .isEqual(OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS)));
        assertEquals(user01.getKey(), changes.getFirst().getChanges().getValueChanges().getFirst().getEntityKey());
        assertTrue(changes.getFirst()
                .getChanges()
                .getValueChanges()
                .stream()
                .anyMatch(c -> c.getField().equals("username") && c.getOldValues().contains("testuser_v1")
                        && PropertyChangeType.PROPERTY_VALUE_CHANGED == PropertyChangeType.valueOf(c.getChangeType())));
        assertTrue(changes.getFirst()
                .getChanges()
                .getValueChanges()
                .stream()
                .anyMatch(c -> c.getField().equals("username") && c.getNewValues().contains("testuser_v2")
                        && PropertyChangeType.PROPERTY_VALUE_CHANGED == PropertyChangeType.valueOf(c.getChangeType())));
        assertTrue(changes.getFirst()
                .getChanges()
                .getValueChanges()
                .stream()
                .anyMatch(c -> c.getField().equals("plainAttrs[email]") && c.getNewValues()
                        .contains("new.email@example.com") && c.getOldValues().contains("old.email@example.com")));
        assertTrue(changes.getFirst()
                .getChanges()
                .getValueChanges()
                .stream()
                .anyMatch(c -> c.getField().equals("resources") && c.getOldValues().contains("LDAP_RESOURCE_v1")
                        && c.getNewValues().contains("LDAP_RESOURCE_v2")));
        assertTrue(changes.getFirst()
                .getChanges()
                .getValueChanges()
                .stream()
                .anyMatch(c -> c.getField().equals("roles") && c.getOldValues().contains("User manager_v1")
                        && c.getNewValues().contains("User manager_v2")));
        assertTrue(changes.getFirst()
                .getChanges()
                .getValueChanges()
                .stream()
                .anyMatch(c -> c.getField().equals("auxClasses") && c.getOldValues().contains("other_v1")
                        && c.getNewValues().contains("other_v2")));
    }

    @Test
    public void searchForGroupSnapshots() {
        GroupTO group01 = new GroupTO();
        group01.setKey(UUID.randomUUID().toString());
        group01.setName("testgrp");

        List<Shadow<GroupTO>> shadows0To100 = generateGroupShadows(group01, 0, 100);
        List<Shadow<GroupTO>> shadows100To200 = generateGroupShadows(group01, 100, 190);

        when(javers.findShadowsAndStream(ArgumentMatchers.any(JqlQuery.class))).thenAnswer(ic -> shadows0To100.stream())
                .thenAnswer(ic -> shadows100To200.stream());

        // first page
        Page<ShadowTO<GroupTO>> events0To100 =
                auditEventDAO.searchForShadows(group01.getKey(), null, null, PageRequest.of(0, 100), GroupTO.class);
        assertEquals(100, events0To100.getTotalElements());
        assertTrue(events0To100.getContent()
                .getFirst()
                .getWhen()
                .truncatedTo(ChronoUnit.HOURS)
                .isEqual(OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS)));
        assertEquals("admin_of_testgrp_v0", events0To100.getContent().getFirst().getWho());
        assertEquals("testgrp_v0", events0To100.getContent().getFirst().getAnyTO().getName());
        assertEquals("testgrp_v11", events0To100.getContent().get(11).getAnyTO().getName());
        // second page
        Page<ShadowTO<GroupTO>> events100To200 =
                auditEventDAO.searchForShadows(group01.getKey(), null, null, PageRequest.of(1, 100), GroupTO.class);
        assertEquals(90, events100To200.getTotalElements());
        assertEquals("testgrp_v185", events100To200.getContent().get(85).getAnyTO().getName());
    }

    @Test
    public void searchForAnyObjectSnapshots() {
        AnyObjectTO anyObject01 = new AnyObjectTO();
        anyObject01.setKey(UUID.randomUUID().toString());
        anyObject01.setName("testanyobject");

        List<Shadow<AnyObjectTO>> shadows0To100 = generateAnyObjectShadows(anyObject01, 0, 100);
        List<Shadow<AnyObjectTO>> shadows100To200 = generateAnyObjectShadows(anyObject01, 100, 190);

        when(javers.findShadowsAndStream(ArgumentMatchers.any(JqlQuery.class))).thenAnswer(ic -> shadows0To100.stream())
                .thenAnswer(ic -> shadows100To200.stream());

        // first page
        Page<ShadowTO<AnyObjectTO>> events0To100 =
                auditEventDAO.searchForShadows(anyObject01.getKey(), null, null, PageRequest.of(0, 100),
                        AnyObjectTO.class);
        assertEquals(100, events0To100.getTotalElements());
        assertTrue(events0To100.getContent()
                .getFirst()
                .getWhen()
                .truncatedTo(ChronoUnit.HOURS)
                .isEqual(OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS)));
        assertEquals("admin_of_testanyobject_v0", events0To100.getContent().getFirst().getWho());
        assertEquals("testanyobject_v0", events0To100.getContent().getFirst().getAnyTO().getName());
        assertEquals("testanyobject_v11", events0To100.getContent().get(11).getAnyTO().getName());
        // second page
        Page<ShadowTO<AnyObjectTO>> events100To200 =
                auditEventDAO.searchForShadows(anyObject01.getKey(), null, null, PageRequest.of(1, 100),
                        AnyObjectTO.class);
        assertEquals(90, events100To200.getTotalElements());
        assertEquals("testanyobject_v185", events100To200.getContent().get(85).getAnyTO().getName());
    }

    private List<Shadow<UserTO>> generateUserShadows(final UserTO user01, final int start, final int end) {
        List<Shadow<UserTO>> shadows = new ArrayList<>();
        for (int i = start; i < end; i++) {
            UserTO userVersion = new UserTO();
            userVersion.setKey(user01.getKey());
            userVersion.setUsername("testuser_v" + i);
            Shadow mockShadow = mock(Shadow.class);
            when(mockShadow.get()).thenReturn(userVersion);
            CdoSnapshot snapshot = mock(CdoSnapshot.class);
            GlobalId globalId = mock(GlobalId.class);
            when(globalId.value()).thenReturn(UUID.randomUUID().toString());
            when(snapshot.getGlobalId()).thenReturn(globalId);
            SnapshotType snapshotType = mock(SnapshotType.class);
            when(snapshot.getType()).thenReturn(snapshotType);
            when(snapshotType.name()).thenReturn(UserTO.class.getSimpleName());
            when(mockShadow.getCdoSnapshot()).thenReturn(snapshot);
            shadows.add(mockShadow);
            CommitMetadata commitMetadata = Mockito.mock(CommitMetadata.class);
            when(commitMetadata.getCommitDate()).thenReturn(LocalDateTime.now());
            when(commitMetadata.getAuthor()).thenReturn("admin_of_" + userVersion.getUsername());
            when(mockShadow.getCommitMetadata()).thenReturn(commitMetadata);
        }
        return shadows;
    }

    private Changes generateUserChanges(final UserTO user01) {
        int version = 2;
        Changes mockChanges = mock(Changes.class);
        UserTO user = new UserTO();
        user.setKey(user01.getKey());
        user.setUsername("testuser_v1");
        List<ChangesByCommit> changesByCommitList = new ArrayList<>();

        ChangesByCommit changesByCommit = mock(ChangesByCommit.class);
        CommitMetadata commit = mock(CommitMetadata.class);
        CommitId commitId = mock(CommitId.class);
        when(commitId.value()).thenReturn(UUID.randomUUID().toString());
        when(commit.getId()).thenReturn(commitId);
        when(changesByCommit.getCommit()).thenReturn(commit);
        when(commit.getAuthor()).thenReturn("admin_of_" + user.getUsername());
        when(commit.getCommitDate()).thenReturn(LocalDateTime.now());
        when(commit.getProperties()).thenReturn(Map.of("propertyOne", "propertyOneValue"));

        List<Change> propertyChanges = new ArrayList<>();
        propertyChanges.add(createPropertyChange(user.getKey(), "username", "testuser_v1", "testuser_v2", version));
        propertyChanges.add(createAttrChange(user.getKey(), "email", List.of("old.email@example.com"),
                List.of("new.email@example.com"), version));
        propertyChanges.add(createPropertyListChange(user.getKey(), "LDAP_RESOURCE", "resources", version));
        propertyChanges.add(createPropertyListChange(user.getKey(), "User manager", "roles", version));
        propertyChanges.add(createPropertyListChange(user.getKey(), "other", "auxClasses", version));

        when(changesByCommit.get()).thenReturn(propertyChanges);

        changesByCommitList.add(changesByCommit);

        when(mockChanges.groupByCommit()).thenReturn(changesByCommitList);
        return mockChanges;
    }

    private List<Shadow<GroupTO>> generateGroupShadows(final GroupTO grp, final int start, final int end) {
        List<Shadow<GroupTO>> shadows = new ArrayList<>();
        for (int i = start; i < end; i++) {
            GroupTO grpVersion = new GroupTO();
            grpVersion.setKey(grp.getKey());
            grpVersion.setName("testgrp_v" + i);
            Shadow mockShadow = mock(Shadow.class);
            when(mockShadow.get()).thenReturn(grpVersion);
            CdoSnapshot snapshot = mock(CdoSnapshot.class);
            GlobalId globalId = mock(GlobalId.class);
            when(globalId.value()).thenReturn(UUID.randomUUID().toString());
            when(snapshot.getGlobalId()).thenReturn(globalId);
            SnapshotType snapshotType = mock(SnapshotType.class);
            when(snapshot.getType()).thenReturn(snapshotType);
            when(snapshotType.name()).thenReturn(GroupTO.class.getSimpleName());
            when(mockShadow.getCdoSnapshot()).thenReturn(snapshot);
            shadows.add(mockShadow);
            CommitMetadata commitMetadata = Mockito.mock(CommitMetadata.class);
            when(commitMetadata.getCommitDate()).thenReturn(LocalDateTime.now());
            when(commitMetadata.getAuthor()).thenReturn("admin_of_" + grpVersion.getName());
            when(mockShadow.getCommitMetadata()).thenReturn(commitMetadata);
        }
        return shadows;
    }

    private List<Shadow<AnyObjectTO>> generateAnyObjectShadows(
            final AnyObjectTO anyObject,
            final int start,
            final int end) {
        List<Shadow<AnyObjectTO>> shadows = new ArrayList<>();
        for (int i = start; i < end; i++) {
            AnyObjectTO anyObjectVersion = new AnyObjectTO();
            anyObjectVersion.setKey(anyObject.getKey());
            anyObjectVersion.setName("testanyobject_v" + i);
            Shadow mockShadow = mock(Shadow.class);
            when(mockShadow.get()).thenReturn(anyObjectVersion);
            CdoSnapshot snapshot = mock(CdoSnapshot.class);
            GlobalId globalId = mock(GlobalId.class);
            when(globalId.value()).thenReturn(UUID.randomUUID().toString());
            when(snapshot.getGlobalId()).thenReturn(globalId);
            SnapshotType snapshotType = mock(SnapshotType.class);
            when(snapshot.getType()).thenReturn(snapshotType);
            when(snapshotType.name()).thenReturn(AnyObjectTO.class.getSimpleName());
            when(mockShadow.getCdoSnapshot()).thenReturn(snapshot);
            shadows.add(mockShadow);
            CommitMetadata commitMetadata = Mockito.mock(CommitMetadata.class);
            when(commitMetadata.getCommitDate()).thenReturn(LocalDateTime.now());
            when(commitMetadata.getAuthor()).thenReturn("admin_of_" + anyObjectVersion.getName());
            when(mockShadow.getCommitMetadata()).thenReturn(commitMetadata);
        }
        return shadows;
    }

    private PropertyChange createPropertyChange(
            final String userKey,
            final String propertyName,
            final String oldValue,
            final String newValue,
            final int version) {
        ValueChange attrChange = Mockito.mock(ValueChange.class);
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.value()).thenReturn(userKey);
        when(attrChange.getAffectedGlobalId()).thenReturn(globalId);
        when(attrChange.getPropertyName()).thenReturn(propertyName);
        when(attrChange.getLeft()).thenReturn(oldValue);
        when(attrChange.getRight()).thenReturn(newValue);
        when(attrChange.getChangeType()).thenReturn(PropertyChangeType.PROPERTY_VALUE_CHANGED);

        CommitMetadata commitMetadata = createMockCommitMetadata(version);
        lenient().when(attrChange.getCommitMetadata()).thenReturn(Optional.of(commitMetadata));

        return attrChange;
    }

    private PropertyChange createAttrChange(
            final String userKey,
            final String attrName,
            final List<String> oldValues,
            final List<String> newValues,
            final int version) {
        CollectionChange<?> attrChange = Mockito.mock(CollectionChange.class);
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.value()).thenReturn(userKey);
        when(attrChange.getLeft()).thenAnswer(ir -> List.of());
        when(attrChange.getAffectedGlobalId()).thenReturn(globalId);
        when(attrChange.getPropertyName()).thenReturn("plainAttrs");
        List<ValueAdded> addedAttrs = new ArrayList<>();
        ValueAdded valueAdded = Mockito.mock(ValueAdded.class);
        when(valueAdded.getAddedValue()).thenReturn(new Attr.Builder(attrName).values(newValues).build());
        addedAttrs.add(valueAdded);
        when(attrChange.getValueAddedChanges()).thenReturn(addedAttrs);
        List<ValueRemoved> removedAttrs = new ArrayList<>();
        ValueRemoved valueRemoved = Mockito.mock(ValueRemoved.class);
        removedAttrs.add(valueRemoved);
        when(valueRemoved.getRemovedValue()).thenReturn(new Attr.Builder(attrName).values(oldValues).build());
        when(attrChange.getValueRemovedChanges()).thenReturn(removedAttrs);
        when(attrChange.getChangeType()).thenReturn(PropertyChangeType.PROPERTY_VALUE_CHANGED);

        CommitMetadata commitMetadata = createMockCommitMetadata(version);
        lenient().when(attrChange.getCommitMetadata()).thenReturn(Optional.of(commitMetadata));

        return attrChange;
    }

    private SetChange createPropertyListChange(
            final String userKey,
            final String value,
            final String fieldName,
            final int version) {
        SetChange setChange = Mockito.mock(SetChange.class);
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.value()).thenReturn(userKey);
        when(setChange.getAffectedGlobalId()).thenReturn(globalId);
        when(setChange.getPropertyName()).thenReturn(fieldName);
        lenient().when(setChange.getRight()).thenAnswer(ir -> List.of(value + "_v" + version));
        lenient().when(setChange.getLeft()).thenAnswer(ir -> List.of(value + "_v" + (version - 1)));
        ValueAdded valueAdded = Mockito.mock(ValueAdded.class);
        lenient().when(valueAdded.getAddedValue()).thenReturn(value + "_v" + version);
        lenient().when(setChange.getChanges()).thenReturn(List.of(valueAdded));
        when(setChange.getChangeType()).thenReturn(PropertyChangeType.PROPERTY_VALUE_CHANGED);

        CommitMetadata commitMetadata = createMockCommitMetadata(version);
        lenient().when(setChange.getCommitMetadata()).thenReturn(Optional.of(commitMetadata));

        return setChange;
    }

    private CommitMetadata createMockCommitMetadata(final int version) {
        CommitMetadata commitMetadata = Mockito.mock(CommitMetadata.class);
        lenient().when(commitMetadata.getAuthor()).thenReturn("admin_of_testuser_v" + version);
        lenient().when(commitMetadata.getCommitDate()).thenReturn(LocalDateTime.now().minusHours(version));

        return commitMetadata;
    }

}
