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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.GenericType;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.JaversAuditService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class JaversITCase extends AbstractITCase {

    private static Boolean ENABLED;

    @Test
    public void crudEvents() {
        GroupTO otherchild = GROUP_SERVICE.read("f779c0d4-633b-4be5-8f57-32eb478a3ca5");
        GroupTO additional = GROUP_SERVICE.read("034740a9-fa10-453b-af37-dc7897e98fb1");
        GroupTO artDirector = GROUP_SERVICE.read("ece66293-8f31-4a84-8e8d-23da36e70846");

        UserCR userCR = UserITCase.getUniqueSample("g.h@t.com");
        userCR.getResources().add(RESOURCE_NAME_NOPROPAGATION);
        userCR.getResources().add(RESOURCE_NAME_NOPROPAGATION2);
        userCR.getRoles().add("User manager");
        userCR.getAuxClasses().add("other");
        userCR.getMemberships().add(new MembershipTO.Builder(otherchild.getKey()).build());
        userCR.getMemberships()
                .add(new MembershipTO.Builder(additional.getKey()).plainAttrs(attr("aLong", "1"), attr("cool", "false"))
                        .build());

        UserTO userTO = createUser(userCR).getEntity();
        assertEquals(2, userTO.getMemberships().size());

        String userKey = userTO.getKey();
        try {
            UserUR userUR = new UserUR();
            userUR.setKey(userKey);
            userUR.setPassword(new PasswordPatch.Builder().value("new2Password").build());

            String oldUserId = userTO.getPlainAttr("userId").orElseThrow().getValues().getFirst();
            String oldFullname = userTO.getPlainAttr("fullname").orElseThrow().getValues().getFirst();

            String newUserId = getUUIDString() + "t.w@spre.net";
            userUR.getPlainAttrs().add(attrAddReplacePatch("userId", newUserId));

            String newFullName = getUUIDString() + "g.h@t.com";
            userUR.getPlainAttrs().add(attrAddReplacePatch("fullname", newFullName));
            userUR.getPlainAttrs()
                    .add(new AttrPatch.Builder(new Attr.Builder("loginDate").build()).operation(PatchOperation.DELETE)
                            .build());

            // removing otherchild
            userUR.getMemberships()
                    .add(new MembershipUR.Builder(otherchild.getKey()).operation(PatchOperation.DELETE).build());
            // updating additional plain attrs, aLong removed, activationDate and ctype added, cool updated
            userUR.getMemberships()
                    .add(new MembershipUR.Builder(additional.getKey()).operation(PatchOperation.ADD_REPLACE)
                            .plainAttrs(attr("cool", "true"), attr("activationDate", "2025-12-24T00:00:00.000+0000"),
                                    attr("ctype", "actype"))
                            .build());
            // adding artDirector
            userUR.getMemberships()
                    .add(new MembershipUR.Builder(artDirector.getKey()).operation(PatchOperation.ADD_REPLACE).build());

            userTO = updateUser(userUR).getEntity();
            assertNotNull(userTO);

            // second update: update firstname
            updateUser(new UserUR.Builder(userKey).plainAttr(
                    attrAddReplacePatch("firstname", getUUIDString() + "newfirstname")).build());

            // 1. search and test shadows
            PagedResult<ShadowTO<UserTO>> shadows = JAVERS_AUDIT_USER_SERVICE.shadows(userKey, 1, 25);
            assertEquals(3, shadows.getTotalCount());
            assertEquals(3, shadows.getResult().size());
            ShadowTO<UserTO> shadowCommit1 = shadows.getResult()
                    .stream()
                    .filter(shadow -> "INITIAL".equals(shadow.getType()) && shadow.getVersion() == 1L)
                    .findFirst()
                    .orElseThrow();
            assertEquals("admin", shadowCommit1.getWho());
            assertEquals(userTO.getUsername(), shadowCommit1.getAnyTO().getUsername());
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("userId").isPresent());
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("userId").get().getValues().contains(oldUserId));
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("fullname").isPresent());
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("fullname").get().getValues().contains(oldFullname));
            assertFalse(shadowCommit1.getAnyTO().getMemberships().isEmpty());
            assertTrue(shadowCommit1.getAnyTO().getMembership(otherchild.getKey()).isPresent());
            assertTrue(shadowCommit1.getAnyTO().getMembership(additional.getKey()).isPresent());
            assertFalse(shadowCommit1.getAnyTO().getMembership(artDirector.getKey()).isPresent());

            ShadowTO<UserTO> shadowCommit2 = shadows.getResult()
                    .stream()
                    .filter(shadow -> "UPDATE".equals(shadow.getType()) && shadow.getVersion() == 2L)
                    .findFirst()
                    .orElseThrow();
            assertEquals("admin", shadowCommit2.getWho());
            assertEquals(userTO.getUsername(), shadowCommit1.getAnyTO().getUsername());
            assertTrue(shadowCommit2.getAnyTO().getPlainAttr("userId").isPresent());
            assertTrue(shadowCommit2.getAnyTO().getPlainAttr("userId").get().getValues().contains(newUserId));
            assertTrue(shadowCommit2.getAnyTO().getPlainAttr("fullname").isPresent());
            assertTrue(shadowCommit2.getAnyTO()
                    .getPlainAttr("fullname")
                    .get()
                    .getValues()
                    .containsAll(userTO.getPlainAttr("fullname").orElseThrow().getValues()));
            assertFalse(shadowCommit2.getAnyTO().getMemberships().isEmpty());
            assertFalse(shadowCommit2.getAnyTO().getMembership(otherchild.getKey()).isPresent());
            assertTrue(shadowCommit2.getAnyTO().getMembership(additional.getKey()).isPresent());
            assertTrue(shadowCommit2.getAnyTO().getMembership(artDirector.getKey()).isPresent());
            assertTrue(shadowCommit2.getAnyTO()
                    .getMembership(additional.getKey())
                    .get()
                    .getPlainAttr("ctype")
                    .orElseThrow()
                    .getValues()
                    .contains("actype"));
            assertTrue(
                    shadowCommit2.getAnyTO().getMembership(additional.getKey()).get().getPlainAttr("aLong").isEmpty());
            assertTrue(shadowCommit2.getAnyTO()
                    .getMembership(additional.getKey())
                    .get()
                    .getPlainAttr("activationDate")
                    .orElseThrow()
                    .getValues()
                    .contains("2025-12-24T00:00:00.000+0000"));
            assertTrue(shadowCommit2.getAnyTO()
                    .getMembership(additional.getKey())
                    .get()
                    .getPlainAttr("cool")
                    .orElseThrow()
                    .getValues()
                    .contains("true"));

            // 2. search by entity key
            List<ChangesByCommitTO> changes = JAVERS_AUDIT_USER_SERVICE.changes(userKey, "admin", null, null, 1, 25);
            assertFalse(changes.isEmpty());
            assertTrue(changes.stream()
                    .allMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .allMatch(vc -> vc.getEntityKey().contains(userKey))));
            // search by a different author
            assertTrue(JAVERS_AUDIT_USER_SERVICE.changes(userKey, "bellini", null, null, 1, 25).isEmpty());

            // update with user bellini
            RoleTO role = new RoleTO();
            role.getRealms().add(SyncopeConstants.ROOT_REALM);
            role.setKey("manager");
            role.getEntitlements().add("USER_CREATE");
            role.getEntitlements().add("USER_UPDATE");
            role.getEntitlements().add("USER_SEARCH");
            role.getEntitlements().add("ANYTYPECLASS_READ");
            role.getEntitlements().add("ANYTYPE_LIST");
            role.getEntitlements().add("ANYTYPECLASS_LIST");
            role.getEntitlements().add("RELATIONSHIPTYPE_LIST");
            role.getEntitlements().add("USER_READ");
            role.getEntitlements().add("ANYTYPE_READ");
            role.getEntitlements().add("REALM_SEARCH");
            role.getEntitlements().add("GROUP_SEARCH");
            role = createRole(role);

            updateUser(new UserUR.Builder(USER_SERVICE.read("bellini").getKey()).role(
                    new StringPatchItem.Builder().value(role.getKey()).build()).build());

            CLIENT_FACTORY.create("bellini", ADMIN_PWD)
                    .getService(UserService.class)
                    .update(new UserUR.Builder(userKey).plainAttr(attrAddReplacePatch("firstname", "updated_firstname"))
                            .build());
            // now there is a single change by author
            assertEquals(1, JAVERS_AUDIT_USER_SERVICE.changes(userKey, "bellini", null, null, 1, 25).size());

            // search by author only, without the entity key
            List<ChangesByCommitTO> belliniChanges =
                    JAVERS_AUDIT_USER_SERVICE.changes(null, "bellini", null, null, 1, 25);
            //            assertEquals(1, belliniChanges.size());
            assertTrue(belliniChanges.size() > 0);
            assertTrue(belliniChanges.getFirst()
                    .getChanges()
                    .getValueChanges()
                    .getFirst()
                    .getEntityKey()
                    .contains(userKey));

            try {
                // remove mandatory attribute surname to have the request rejected, changes shouldn't be audited
                CLIENT_FACTORY.create("bellini", ADMIN_PWD)
                        .getService(UserService.class)
                        .update(new UserUR.Builder(userKey).plainAttr(
                                new AttrPatch.Builder(new Attr.Builder("surname").build()).operation(
                                        PatchOperation.DELETE).build()).build());
            } catch (SyncopeClientException sce) {
            }

            belliniChanges = JAVERS_AUDIT_USER_SERVICE.changes(userKey, "bellini", null, null, 1, 25);
            //            assertEquals(1, belliniChanges.size());
            assertTrue(belliniChanges.size() > 0);
            assertTrue(belliniChanges.getFirst()
                    .getChanges()
                    .getValueChanges()
                    .getFirst()
                    .getEntityKey()
                    .contains(userKey));

            updateUser(new UserUR.Builder(userTO.getKey()).resources(
                            new StringPatchItem.Builder().value(RESOURCE_NAME_NOPROPAGATION)
                                    .operation(PatchOperation.DELETE)
                                    .build(), new StringPatchItem.Builder().value(RESOURCE_NAME_NOPROPAGATION2)
                                    .operation(PatchOperation.DELETE)
                                    .build(), new StringPatchItem.Builder().value(RESOURCE_NAME_LDAP).build())
                    .roles(new StringPatchItem.Builder().value("User manager").operation(PatchOperation.DELETE).build(),
                            new StringPatchItem.Builder().value("User reviewer").build())
                    .auxClasses(new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("other").build(),
                            new StringPatchItem.Builder().value("csv").build())
                    .build());
            shadows = JAVERS_AUDIT_USER_SERVICE.shadows(userKey, 1, 25);
            assertEquals(5, shadows.getTotalCount());
            assertEquals(5, shadows.getResult().size());

            changes = JAVERS_AUDIT_USER_SERVICE.changes(userKey, "admin", null, null, 1, 25);
            assertFalse(changes.isEmpty());
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "resources".equals(vc.getField()) && vc.getNewValues()
                                    .contains(RESOURCE_NAME_LDAP) && vc.getOldValues()
                                    .contains(RESOURCE_NAME_NOPROPAGATION) && vc.getOldValues()
                                    .contains(RESOURCE_NAME_NOPROPAGATION2))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "roles".equals(vc.getField()) && vc.getNewValues().contains("User reviewer")
                                    && vc.getOldValues().contains("User manager"))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "auxClasses".equals(vc.getField()) && vc.getNewValues().contains("csv")
                                    && vc.getOldValues().contains("other"))));
        } finally {
            // delete user and generate a delete event
            USER_SERVICE.delete(userKey);
        }
        PagedResult<ShadowTO<UserTO>> shadows = JAVERS_AUDIT_USER_SERVICE.shadows(userKey, 1, 25);
        assertEquals(6, shadows.getTotalCount());
        assertEquals(6, shadows.getResult().size());
        Optional<ShadowTO<UserTO>> shadowCommit6 = shadows.getResult()
                .stream()
                .filter(shadow -> "TERMINAL".equals(shadow.getType()) && shadow.getVersion() == 6L)
                .findFirst();
        assertTrue(shadowCommit6.isPresent());
        assertEquals("admin", shadowCommit6.get().getWho());
    }

    @Test
    void pullEvents() {
        // modifica da evento di pull tracciata
        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setResource(RESOURCE_NAME_LDAP);
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setRemediation(true);
        pullTask.setPerformCreate(true);
        pullTask.setPerformUpdate(true);
        pullTask.setUnmatchingRule(UnmatchingRule.ASSIGN);
        pullTask.setMatchingRule(MatchingRule.UPDATE);

        RECONCILIATION_SERVICE.pull(
                new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).fiql("uid==pullFromLDAP").build(),
                pullTask);

        UserTO pullFromLDAP = USER_SERVICE.read("pullFromLDAP");

        PagedResult<ShadowTO<UserTO>> shadows = JAVERS_AUDIT_USER_SERVICE.shadows(pullFromLDAP.getKey(), 1, 25);
        assertEquals(1, shadows.getTotalCount());
        assertEquals(1, shadows.getResult().size());
        ShadowTO<UserTO> shadowCommit1 = shadows.getResult()
                .stream()
                .filter(shadow -> "INITIAL".equals(shadow.getType()) && shadow.getVersion() == 1L)
                .findFirst()
                .orElseThrow();
        assertEquals("admin", shadowCommit1.getWho());
        assertFalse(shadowCommit1.getAdditionalInfo().isEmpty());
        assertTrue(shadowCommit1.getAdditionalInfo().get("context").contains("PULL Task"));

        // re-pull and generate an update
        RECONCILIATION_SERVICE.pull(
                new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).fiql("uid==pullFromLDAP").build(),
                pullTask);
        shadows = JAVERS_AUDIT_USER_SERVICE.shadows(pullFromLDAP.getKey(), 1, 25);
        assertEquals(2, shadows.getTotalCount());
        assertEquals(2, shadows.getResult().size());
    }

    @Test
    void eventsOnTwoDomain() {
        SyncopeClientFactoryBean twoCF = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("Two");
        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            twoCF.setContentType(envContentType);
        }
        SyncopeClient twoSC = twoCF.create(ADMIN_UNAME, "password2");
        UserService twoUS = twoSC.getService(UserService.class);

        UserTO userTO = twoUS.create(UserITCase.getUniqueSample("g.h@t.com"))
                .readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                })
                .getEntity();

        // 1. search and test shadows
        PagedResult<ShadowTO<UserTO>> shadows = twoCF.create(ADMIN_UNAME, "password2")
                .getService(JaversAuditService.class)
                .shadows(userTO.getKey(), 1, 25);
        assertEquals(1, shadows.getTotalCount());
        assertEquals(1, shadows.getResult().size());
        ShadowTO<UserTO> shadowCommit1 = shadows.getResult()
                .stream()
                .filter(shadow -> "INITIAL".equals(shadow.getType()) && shadow.getVersion() == 1L)
                .findFirst()
                .orElseThrow();
        assertEquals("admin", shadowCommit1.getWho());
        assertEquals(userTO.getUsername(), shadowCommit1.getAnyTO().getUsername());
        assertTrue(shadowCommit1.getAnyTO().getPlainAttr("email").isPresent());
        assertTrue(shadowCommit1.getAnyTO()
                .getPlainAttr("email")
                .get()
                .getValues()
                .containsAll(userTO.getPlainAttr("email").orElseThrow().getValues()));
    }

}
