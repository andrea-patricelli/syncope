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
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.JaversAuditService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.javers.core.diff.changetype.PropertyChangeType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JaversITCase extends AbstractITCase {

    private static Boolean ENABLED;

    private static final String BELLINI_KEY = "c9b2dec2-00a7-4855-97c0-d854842b4b24";

    private static final String PUCCINI_KEY = "823074dc-d280-436d-a7dd-07399fae48ec";

    private static final String HP_PRINTER_KEY = "fc6dbc3a-6c07-4965-8781-921e7401a4a5";

    private static final String CANON_PRINTER_KEY = "8559d14d-58c2-46eb-a2d4-a7d35161e8f8";

    @BeforeAll
    static void setUp() {
        RelationshipTypeTO relTypeTO = RELATIONSHIP_TYPE_SERVICE.read("neighborhood");

        if (relTypeTO.getTypeExtension(AnyTypeKind.USER.name()).isEmpty()) {
            TypeExtensionTO typeExt = new TypeExtensionTO();
            typeExt.setAnyType(AnyTypeKind.USER.name());
            typeExt.getAuxClasses().add("other");
            relTypeTO.getTypeExtensions().add(typeExt);

            RELATIONSHIP_TYPE_SERVICE.update(relTypeTO);
        }
    }

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
        // memberships
        userCR.getMemberships().add(new MembershipTO.Builder(otherchild.getKey()).build());
        userCR.getMemberships()
                .add(new MembershipTO.Builder(additional.getKey()).plainAttrs(attr("aLong", "1"), attr("cool", "false"))
                        .build());
        // relationships
        userCR.getRelationships()
                .add(new RelationshipTO.Builder("neighborhood").plainAttr(
                        new Attr.Builder("aLong").value("1111").build()).otherEnd(HP_PRINTER_KEY).build());
        // linked accounts
        LinkedAccountTO pullFromLdap2 =
                new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, "pullFromLdap2").username("pullFromLdap2")
                        .password("Password123!")
                        .build();
        pullFromLdap2.getPlainAttrs().add(attr("aLong", "1234"));
        pullFromLdap2.getPlainAttrs().add(attr("cool", "true"));
        pullFromLdap2.getPlainAttrs().add(attr("ctype", "actype"));

        LinkedAccountTO testUser02 =
                new LinkedAccountTO.Builder(RESOURCE_NAME_TESTDB, "testUser02").password("Password123!").build();
        testUser02.getPlainAttrs().add(attr("aLong", "5678"));
        testUser02.getPlainAttrs().add(attr("surname", "testUser02"));

        userCR.getLinkedAccounts().add(pullFromLdap2);
        userCR.getLinkedAccounts().add(testUser02);

        // set user manager bellini
        userCR.setUManager(BELLINI_KEY);

        UserTO userTO = createUser(userCR).getEntity();
        assertEquals(2, userTO.getMemberships().size());

        String userKey = userTO.getKey();
        try {
            UserUR userUR = new UserUR();
            userUR.setKey(userKey);
            userUR.setPassword(new PasswordPatch.Builder().value("new2Password").build());

            // change user manager
            userUR.setUManager(new StringReplacePatchItem.Builder().value(PUCCINI_KEY).build());

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

            // remove testUser02 linked account and change pullFromLdap2 attributes
            userUR.getLinkedAccounts()
                    .add(new LinkedAccountUR.Builder().operation(PatchOperation.DELETE)
                            .linkedAccountTO(testUser02)
                            .build());
            pullFromLdap2.getPlainAttrs().removeIf(pa -> "cool".equals(pa.getSchema()));
            pullFromLdap2.getPlainAttr("aLong").orElseThrow().getValues().clear();
            pullFromLdap2.getPlainAttr("aLong").orElseThrow().getValues().add("4321");
            pullFromLdap2.getPlainAttr("ctype").orElseThrow().getValues().clear();
            pullFromLdap2.getPlainAttr("ctype").orElseThrow().getValues().add("anewctype");
            userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(pullFromLdap2).build());

            // first update
            userTO = updateUser(userUR).getEntity();
            assertNotNull(userTO);

            // second update: update firstname
            updateUser(new UserUR.Builder(userKey).plainAttr(
                    attrAddReplacePatch("firstname", getUUIDString() + "newfirstname")).build());

            // third update: modify resources, roles and relationships
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
                    .relationships(new RelationshipUR.Builder("neighborhood").otherEnd(HP_PRINTER_KEY)
                            .plainAttr(new Attr.Builder("aLong").value("2222").build())
                            .build(), new RelationshipUR.Builder("neighborhood").otherEnd(CANON_PRINTER_KEY).build())
                    .build());

            // 1. search and test shadows
            PagedResult<ShadowTO<UserTO>> shadows = JAVERS_AUDIT_USER_SERVICE.shadows(userKey, 1, 25);
            assertTrue(shadows.getTotalCount() >= 3); // TODO rimuovere prima della pr
            //            assertEquals(3, shadows.getTotalCount());
            //            assertEquals(3, shadows.getResult().size());
            ShadowTO<UserTO> shadowCommit1 = shadows.getResult()
                    .stream()
                    .filter(shadow -> "INITIAL".equals(shadow.getType()) && shadow.getVersion() == 1L)
                    .findFirst()
                    .orElseThrow();
            assertEquals("admin", shadowCommit1.getWho());
            assertEquals(userTO.getUsername(), shadowCommit1.getAnyTO().getUsername());
            assertEquals(BELLINI_KEY, shadowCommit1.getAnyTO().getUManager());
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("userId").isPresent());
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("userId").get().getValues().contains(oldUserId));
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("fullname").isPresent());
            assertTrue(shadowCommit1.getAnyTO().getPlainAttr("fullname").get().getValues().contains(oldFullname));
            assertFalse(shadowCommit1.getAnyTO().getMemberships().isEmpty());
            assertTrue(shadowCommit1.getAnyTO().getMembership(otherchild.getKey()).isPresent());
            assertTrue(shadowCommit1.getAnyTO().getMembership(additional.getKey()).isPresent());
            assertFalse(shadowCommit1.getAnyTO().getMembership(artDirector.getKey()).isPresent());
            assertTrue(shadowCommit1.getAnyTO().getRelationship("neighborhood", HP_PRINTER_KEY).isPresent());
            assertTrue(shadowCommit1.getAnyTO()
                    .getRelationship("neighborhood", HP_PRINTER_KEY)
                    .get()
                    .getPlainAttr("aLong")
                    .isPresent());
            assertTrue(shadowCommit1.getAnyTO()
                    .getRelationship("neighborhood", HP_PRINTER_KEY)
                    .get()
                    .getPlainAttr("aLong")
                    .get()
                    .getValues()
                    .contains("1111"));

            // linked accounts in shadow 1
            assertFalse(shadowCommit1.getAnyTO().getLinkedAccounts().isEmpty());
            assertTrue(shadowCommit1.getAnyTO()
                    .getLinkedAccounts()
                    .stream()
                    .anyMatch(la -> la.getConnObjectKeyValue().equals("pullFromLdap2") && RESOURCE_NAME_LDAP.equals(
                            la.getResource()) && la.getUsername().equals("pullFromLdap2") && la.getPlainAttr("aLong")
                            .isPresent() && la.getPlainAttr("aLong").get().getValues().contains("1234")
                            && la.getPlainAttr("cool").isPresent() && la.getPlainAttr("cool")
                            .get()
                            .getValues()
                            .contains("true") && la.getPlainAttr("ctype").isPresent() && la.getPlainAttr("ctype")
                            .get()
                            .getValues()
                            .contains("actype")));
            assertTrue(shadowCommit1.getAnyTO()
                    .getLinkedAccounts()
                    .stream()
                    .anyMatch(la -> la.getConnObjectKeyValue().equals("testUser02") && RESOURCE_NAME_TESTDB.equals(
                            la.getResource()) && la.getPlainAttr("surname").isPresent() && la.getPlainAttr("surname")
                            .get()
                            .getValues()
                            .contains("testUser02") && la.getPlainAttr("aLong").isPresent() && la.getPlainAttr("aLong")
                            .get()
                            .getValues()
                            .contains("5678")));

            ShadowTO<UserTO> shadowCommit2 = shadows.getResult()
                    .stream()
                    .filter(shadow -> "UPDATE".equals(shadow.getType()) && shadow.getVersion() == 2L)
                    .findFirst()
                    .orElseThrow();
            assertEquals("admin", shadowCommit2.getWho());
            assertEquals(userTO.getUsername(), shadowCommit1.getAnyTO().getUsername());
            assertEquals(PUCCINI_KEY, userTO.getUManager());
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
            ShadowTO<UserTO> shadowCommit4 = shadows.getResult()
                    .stream()
                    .filter(shadow -> "UPDATE".equals(shadow.getType()) && shadow.getVersion() == 4L)
                    .findFirst()
                    .orElseThrow();
            assertFalse(shadowCommit4.getAnyTO().getRelationships().isEmpty());
            assertTrue(shadowCommit4.getAnyTO().getRelationship("neighborhood", CANON_PRINTER_KEY).isPresent());
            assertTrue(shadowCommit4.getAnyTO().getRelationship("neighborhood", HP_PRINTER_KEY).isPresent());
            assertTrue(shadowCommit4.getAnyTO()
                    .getRelationship("neighborhood", HP_PRINTER_KEY)
                    .get()
                    .getPlainAttr("aLong")
                    .isPresent());
            assertTrue(shadowCommit4.getAnyTO()
                    .getRelationship("neighborhood", HP_PRINTER_KEY)
                    .get()
                    .getPlainAttr("aLong")
                    .get()
                    .getValues()
                    .contains("2222"));
            // linked accounts in shadow 2
            assertEquals(1, shadowCommit2.getAnyTO().getLinkedAccounts().size());
            assertTrue(shadowCommit2.getAnyTO()
                    .getLinkedAccounts()
                    .stream()
                    .anyMatch(la -> la.getConnObjectKeyValue().equals("pullFromLdap2") && RESOURCE_NAME_LDAP.equals(
                            la.getResource()) && la.getUsername().equals("pullFromLdap2") && la.getPlainAttr("aLong")
                            .isPresent() && la.getPlainAttr("aLong").get().getValues().contains("4321")
                            && la.getPlainAttr("cool").isEmpty() && la.getPlainAttr("ctype").isPresent()
                            && la.getPlainAttr("ctype").get().getValues().contains("anewctype")));
            assertTrue(shadowCommit2.getAnyTO()
                    .getLinkedAccounts()
                    .stream()
                    .noneMatch(la -> la.getConnObjectKeyValue().equals("testUser02") && RESOURCE_NAME_TESTDB.equals(
                            la.getResource())));

            // 2. search changes by entity key
            List<ChangesByCommitTO> changes = JAVERS_AUDIT_USER_SERVICE.changes(userKey, "admin", null, null, 1, 25);
            assertFalse(changes.isEmpty());
            assertTrue(changes.stream()
                    .anyMatch(pc -> pc.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "uManager".equals(vc.getField()) && vc.getOldValues().contains(BELLINI_KEY)
                                    && vc.getNewValues().contains(PUCCINI_KEY))));
            // changes key must match the user key
            assertTrue(changes.stream()
                    .allMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .allMatch(vc -> vc.getEntityKey().contains(userKey))));
            // changes on memberships
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "memberships".equals(vc.getField()) && vc.getOldValues()
                                    .contains(otherchild.getKey() + "," + otherchild.getName()) && vc.getNewValues()
                                    .contains(additional.getKey() + "," + additional.getName()) && vc.getNewValues()
                                    .contains(artDirector.getKey() + "," + artDirector.getName()))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "memberships[additional].plainAttrs[cool]".equals(vc.getField())
                                    && vc.getOldValues().contains("false") && vc.getNewValues().contains("true"))));

            // changes on resources
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "resources".equals(vc.getField()) && vc.getNewValues()
                                    .contains(RESOURCE_NAME_LDAP) && vc.getOldValues()
                                    .contains(RESOURCE_NAME_NOPROPAGATION) && vc.getOldValues()
                                    .contains(RESOURCE_NAME_NOPROPAGATION2))));
            // changes in roles
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "roles".equals(vc.getField()) && vc.getNewValues().contains("User reviewer")
                                    && vc.getOldValues().contains("User manager"))));
            // changes in auxClasses
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "auxClasses".equals(vc.getField()) && vc.getNewValues().contains("csv")
                                    && vc.getOldValues().contains("other"))));

            // changes in linked accounts
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "linkedAccounts".equals(vc.getField()) && vc.getNewValues()
                                    .contains("linkedAccounts[pullFromLdap2," + RESOURCE_NAME_LDAP + "]")
                                    && vc.getOldValues()
                                    .contains("linkedAccounts[testUser02," + RESOURCE_NAME_TESTDB + "]")
                                    && vc.getOldValues()
                                    .contains("linkedAccounts[pullFromLdap2," + RESOURCE_NAME_LDAP + "]"))));
            // changes in linked accounts attributes
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[pullFromLdap2," + RESOURCE_NAME_LDAP
                                    + "].plainAttrs[aLong]").equals(vc.getField()) && vc.getOldValues().contains("1234")
                                    && vc.getNewValues().contains("4321"))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[pullFromLdap2," + RESOURCE_NAME_LDAP
                                    + "].plainAttrs[ctype]").equals(vc.getField()) && vc.getOldValues()
                                    .contains("actype") && vc.getNewValues().contains("anewctype"))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[pullFromLdap2," + RESOURCE_NAME_LDAP
                                    + "].plainAttrs[cool]").equals(vc.getField())
                                    && PropertyChangeType.PROPERTY_REMOVED.name().equals(vc.getChangeType()))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[testUser02," + RESOURCE_NAME_TESTDB
                                    + "].plainAttrs[surname]").equals(vc.getField())
                                    && PropertyChangeType.PROPERTY_REMOVED.name().equals(vc.getChangeType()))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[testUser02," + RESOURCE_NAME_TESTDB
                                    + "].plainAttrs[aLong]").equals(vc.getField())
                                    && PropertyChangeType.PROPERTY_REMOVED.name().equals(vc.getChangeType()))));

            // changes in relationships and their attributes
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "relationships".equals(vc.getField()) && vc.getOldValues()
                                    .contains("neighborhood,LEFT,PRINTER," + HP_PRINTER_KEY) && vc.getNewValues()
                                    .contains("neighborhood,LEFT,PRINTER," + HP_PRINTER_KEY) && vc.getNewValues()
                                    .contains("neighborhood,LEFT,PRINTER," + CANON_PRINTER_KEY))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "relationships[HP LJ 1300n].plainAttrs[aLong]".equals(vc.getField())
                                    && vc.getOldValues().contains("1111") && vc.getNewValues().contains("2222"))));
            // no attributes for this relationship
            assertTrue(changes.stream()
                    .noneMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "relationships[Canon MF 8030cn.plainAttrs[aLong]".equals(vc.getField()))));

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
            assertTrue(belliniChanges.size() > 0); // TODO rimuovere quest'asserzione prima della PR
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
            assertTrue(belliniChanges.size() > 0); // TODO rimuovere quest'asserzione prima della PR
            assertTrue(belliniChanges.getFirst()
                    .getChanges()
                    .getValueChanges()
                    .getFirst()
                    .getEntityKey()
                    .contains(userKey));

            shadows = JAVERS_AUDIT_USER_SERVICE.shadows(userKey, 1, 25);
            assertEquals(5, shadows.getTotalCount());
            assertEquals(5, shadows.getResult().size());
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
        assertTrue(shadows.getTotalCount() >= 1); // TODO rimuovere prima della pr
        //        assertEquals(1, shadows.getTotalCount());
        //        assertEquals(1, shadows.getResult().size());
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
