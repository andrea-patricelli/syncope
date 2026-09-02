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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.core.GenericType;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropertyChangeTO;
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
import org.apache.syncope.common.rest.api.service.JaversAuditUserService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class JaversITCase extends AbstractITCase {

    private static final String BELLINI_KEY = "c9b2dec2-00a7-4855-97c0-d854842b4b24";

    private static final String PUCCINI_KEY = "823074dc-d280-436d-a7dd-07399fae48ec";

    private static final String HP_PRINTER_KEY = "fc6dbc3a-6c07-4965-8781-921e7401a4a5";

    private static final String CANON_PRINTER_KEY = "8559d14d-58c2-46eb-a2d4-a7d35161e8f8";

    @BeforeAll
    static void setUp() {
        assumeTrue(IS_JAVERS_ENABLED);

        RelationshipTypeTO relTypeTO = RELATIONSHIP_TYPE_SERVICE.read("neighborhood");

        if (relTypeTO.getTypeExtension(AnyTypeKind.USER.name()).isEmpty()) {
            TypeExtensionTO typeExt = new TypeExtensionTO();
            typeExt.setAnyType(AnyTypeKind.USER.name());
            typeExt.getAuxClasses().add("other");
            relTypeTO.getTypeExtensions().add(typeExt);

            RELATIONSHIP_TYPE_SERVICE.update(relTypeTO);
        }
    }

    @BeforeEach
    public void check() {
        assumeTrue(IS_JAVERS_ENABLED);
    }

    @Test
    public void crudUsers() {
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
        LinkedAccountTO linkedAccountOnNoPropagation =
                new LinkedAccountTO.Builder(RESOURCE_NAME_NOPROPAGATION, "linkedAccountOnNoPropagation").username(
                        "linkedAccountOnNoPropagation").password("Password123!").build();
        linkedAccountOnNoPropagation.getPlainAttrs().add(attr("aLong", "1234"));
        linkedAccountOnNoPropagation.getPlainAttrs().add(attr("cool", "true"));
        linkedAccountOnNoPropagation.getPlainAttrs().add(attr("ctype", "actype"));

        LinkedAccountTO testUser02 =
                new LinkedAccountTO.Builder(RESOURCE_NAME_TESTDB, "testUser02").password("Password123!").build();
        testUser02.getPlainAttrs().add(attr("aLong", "5678"));
        testUser02.getPlainAttrs().add(attr("surname", "testUser02"));

        userCR.getLinkedAccounts().add(linkedAccountOnNoPropagation);
        userCR.getLinkedAccounts().add(testUser02);

        // set user manager bellini
        userCR.setuManager(BELLINI_KEY);

        UserTO userTO = createUser(userCR).getEntity();
        assertEquals(2, userTO.getMemberships().size());

        String userKey = userTO.getKey();
        try {
            UserUR userUR = new UserUR();
            userUR.setKey(userKey);
            userUR.setPassword(new PasswordPatch.Builder().value("new2Password").build());

            // change user manager
            userUR.setuManager(new StringReplacePatchItem.Builder().value(PUCCINI_KEY).build());

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

            // remove testUser02 linked account and change linkedAccountOnNoPropagation attributes
            userUR.getLinkedAccounts()
                    .add(new LinkedAccountUR.Builder().operation(PatchOperation.DELETE)
                            .linkedAccountTO(testUser02)
                            .build());
            linkedAccountOnNoPropagation.getPlainAttrs().removeIf(pa -> "cool".equals(pa.getSchema()));
            linkedAccountOnNoPropagation.getPlainAttr("aLong").orElseThrow().getValues().clear();
            linkedAccountOnNoPropagation.getPlainAttr("aLong").orElseThrow().getValues().add("4321");
            linkedAccountOnNoPropagation.getPlainAttr("ctype").orElseThrow().getValues().clear();
            linkedAccountOnNoPropagation.getPlainAttr("ctype").orElseThrow().getValues().add("anewctype");
            userUR.getLinkedAccounts()
                    .add(new LinkedAccountUR.Builder().linkedAccountTO(linkedAccountOnNoPropagation).build());

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
            assertEquals(4, shadows.getTotalCount());
            assertEquals(4, shadows.getResult().size());
            ShadowTO<UserTO> shadowCommit1 = shadows.getResult()
                    .stream()
                    .filter(shadow -> "INITIAL".equals(shadow.getType()) && shadow.getVersion() == 1L)
                    .findFirst()
                    .orElseThrow();
            assertEquals("admin", shadowCommit1.getWho());
            assertEquals(userTO.getUsername(), shadowCommit1.getAnyTO().getUsername());
            assertEquals(BELLINI_KEY, shadowCommit1.getAnyTO().getuManager());
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
                    .anyMatch(la -> la.getConnObjectKeyValue().equals("linkedAccountOnNoPropagation")
                            && RESOURCE_NAME_NOPROPAGATION.equals(la.getResource()) && la.getUsername()
                            .equals("linkedAccountOnNoPropagation") && la.getPlainAttr("aLong").isPresent()
                            && la.getPlainAttr("aLong").get().getValues().contains("1234") && la.getPlainAttr("cool")
                            .isPresent() && la.getPlainAttr("cool").get().getValues().contains("true")
                            && la.getPlainAttr("ctype").isPresent() && la.getPlainAttr("ctype")
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
            assertEquals(PUCCINI_KEY, userTO.getuManager());
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
                    .anyMatch(la -> la.getConnObjectKeyValue().equals("linkedAccountOnNoPropagation")
                            && RESOURCE_NAME_NOPROPAGATION.equals(la.getResource()) && la.getUsername()
                            .equals("linkedAccountOnNoPropagation") && la.getPlainAttr("aLong").isPresent()
                            && la.getPlainAttr("aLong").get().getValues().contains("4321") && la.getPlainAttr("cool")
                            .isEmpty() && la.getPlainAttr("ctype").isPresent() && la.getPlainAttr("ctype")
                            .get()
                            .getValues()
                            .contains("anewctype")));
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
                                    .contains(
                                            "linkedAccounts[linkedAccountOnNoPropagation," + RESOURCE_NAME_NOPROPAGATION
                                                    + "]") && vc.getOldValues()
                                    .contains("linkedAccounts[testUser02," + RESOURCE_NAME_TESTDB + "]")
                                    && vc.getOldValues()
                                    .contains(
                                            "linkedAccounts[linkedAccountOnNoPropagation," + RESOURCE_NAME_NOPROPAGATION
                                                    + "]"))));
            // changes in linked accounts attributes
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(
                                    vc -> ("linkedAccounts[linkedAccountOnNoPropagation," + RESOURCE_NAME_NOPROPAGATION
                                            + "].plainAttrs[aLong]").equals(vc.getField()) && vc.getOldValues()
                                            .contains("1234") && vc.getNewValues().contains("4321"))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(
                                    vc -> ("linkedAccounts[linkedAccountOnNoPropagation," + RESOURCE_NAME_NOPROPAGATION
                                            + "].plainAttrs[ctype]").equals(vc.getField()) && vc.getOldValues()
                                            .contains("actype") && vc.getNewValues().contains("anewctype"))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(
                                    vc -> ("linkedAccounts[linkedAccountOnNoPropagation," + RESOURCE_NAME_NOPROPAGATION
                                            + "].plainAttrs[cool]").equals(vc.getField())
                                            && "PROPERTY_REMOVED".equals(vc.getChangeType()))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[testUser02," + RESOURCE_NAME_TESTDB
                                    + "].plainAttrs[surname]").equals(vc.getField())
                                    && PropertyChangeTO.PropertyChangeType.PROPERTY_REMOVED.name()
                                    .equals(vc.getChangeType()))));
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> ("linkedAccounts[testUser02," + RESOURCE_NAME_TESTDB
                                    + "].plainAttrs[aLong]").equals(vc.getField())
                                    && PropertyChangeTO.PropertyChangeType.PROPERTY_REMOVED.name()
                                    .equals(vc.getChangeType()))));

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
            assertTrue(JAVERS_AUDIT_USER_SERVICE.changes(userKey, "anotheradmin@syncope.apache.org", null, null, 1, 25)
                    .isEmpty());

            // update with manager user
            RoleTO anotherAdminRole = new RoleTO();
            anotherAdminRole.getRealms().add(SyncopeConstants.ROOT_REALM);
            anotherAdminRole.setKey("manager");
            anotherAdminRole.getEntitlements().add("USER_CREATE");
            anotherAdminRole.getEntitlements().add("USER_UPDATE");
            anotherAdminRole.getEntitlements().add("USER_SEARCH");
            anotherAdminRole.getEntitlements().add("ANYTYPECLASS_READ");
            anotherAdminRole.getEntitlements().add("ANYTYPE_LIST");
            anotherAdminRole.getEntitlements().add("ANYTYPECLASS_LIST");
            anotherAdminRole.getEntitlements().add("RELATIONSHIPTYPE_LIST");
            anotherAdminRole.getEntitlements().add("USER_READ");
            anotherAdminRole.getEntitlements().add("ANYTYPE_READ");
            anotherAdminRole.getEntitlements().add("REALM_SEARCH");
            anotherAdminRole.getEntitlements().add("GROUP_SEARCH");
            anotherAdminRole = createRole(anotherAdminRole);

            userCR = UserITCase.getUniqueSample("anotheradmin@syncope.apache.org");
            userCR.setPassword("Password123!");
            userCR.getRoles().add(anotherAdminRole.getKey());
            UserTO anotherAdmin = createUser(userCR).getEntity();

            CLIENT_FACTORY.create(anotherAdmin.getUsername(), "Password123!")
                    .getService(UserService.class)
                    .update(new UserUR.Builder(userKey).plainAttr(attrAddReplacePatch("firstname", "updated_firstname"))
                            .build());
            // now there is a single change by author
            assertEquals(1,
                    JAVERS_AUDIT_USER_SERVICE.changes(userKey, anotherAdmin.getUsername(), null, null, 1, 25).size());

            // search by author only, without the entity key
            List<ChangesByCommitTO> anotherAdminChanges =
                    JAVERS_AUDIT_USER_SERVICE.changes(null, anotherAdmin.getUsername(), null, null, 1, 25);
            assertEquals(1, anotherAdminChanges.size());
            assertTrue(anotherAdminChanges.getFirst()
                    .getChanges()
                    .getValueChanges()
                    .getFirst()
                    .getEntityKey()
                    .contains(userKey));

            try {
                // remove mandatory attribute surname to have the request rejected, changes shouldn't be audited
                CLIENT_FACTORY.create(anotherAdmin.getUsername(), "Password123!")
                        .getService(UserService.class)
                        .update(new UserUR.Builder(userKey).plainAttr(
                                new AttrPatch.Builder(new Attr.Builder("surname").build()).operation(
                                        PatchOperation.DELETE).build()).build());
            } catch (SyncopeClientException sce) {
            }

            anotherAdminChanges =
                    JAVERS_AUDIT_USER_SERVICE.changes(userKey, anotherAdmin.getUsername(), null, null, 1, 25);
            assertEquals(1, anotherAdminChanges.size());
            assertTrue(anotherAdminChanges.getFirst()
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
        pullTask.setResource(RESOURCE_NAME_TESTDB2);
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setRemediation(true);
        pullTask.setPerformCreate(true);
        pullTask.setPerformUpdate(true);
        pullTask.setSyncStatus(true);
        pullTask.setUnmatchingRule(UnmatchingRule.ASSIGN);
        pullTask.setMatchingRule(MatchingRule.UPDATE);

        RECONCILIATION_SERVICE.pull(
                new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_TESTDB2).fiql("ID==rossini").build(),
                pullTask);

        UserTO rossini = USER_SERVICE.read("rossini");

        PagedResult<ShadowTO<UserTO>> shadows = JAVERS_AUDIT_USER_SERVICE.shadows(rossini.getKey(), 1, 25);
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

        // re-pull and generate an empty update -> no changes means empty commit that is not going to be persisted
        RECONCILIATION_SERVICE.pull(
                new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_TESTDB2).fiql("ID==rossini").build(),
                pullTask);
        shadows = JAVERS_AUDIT_USER_SERVICE.shadows(rossini.getKey(), 1, 25);
        assertEquals(1, shadows.getTotalCount());
        assertEquals(1, shadows.getResult().size());
        JdbcTemplate test2JdbcTemplate = new JdbcTemplate(testDataSource);
        try {
            // generate an update on database and re-pull
            test2JdbcTemplate.update("UPDATE test2 SET status = false WHERE id = 'rossini'");

            // re-pull and generate a real update event
            RECONCILIATION_SERVICE.pull(
                    new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_TESTDB2).fiql("ID==rossini").build(),
                    pullTask);
            shadows = JAVERS_AUDIT_USER_SERVICE.shadows(rossini.getKey(), 1, 25);
            assertEquals(2, shadows.getTotalCount());
            assertEquals(2, shadows.getResult().size());
        } finally {
            test2JdbcTemplate.update("UPDATE test2 SET status = true WHERE id = 'rossini'");
        }
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
                .getService(JaversAuditUserService.class)
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

    @Test
    public void crudGroups() {
        // create a new relationship type to relate group and any objects
        RelationshipTypeTO relationshipType = new RelationshipTypeTO();
        relationshipType.setKey("grp_inclusion");
        relationshipType.setDescription("grp_inclusion");
        relationshipType.setLeftEndAnyType(AnyTypeKind.GROUP.name());
        relationshipType.setRightEndAnyType("PRINTER");
        RELATIONSHIP_TYPE_SERVICE.create(relationshipType);

        GroupCR groupCR = GroupITCase.getSample("javersGrp01");
        groupCR.getResources().add(RESOURCE_NAME_NOPROPAGATION);
        groupCR.getAuxClasses().add("other");
        TypeExtensionTO userTE = new TypeExtensionTO();
        userTE.setAnyType(AnyTypeKind.USER.name());
        userTE.getAuxClasses().add("csv");
        userTE.getAuxClasses().add("generic membership");
        groupCR.getRelationships().add(new RelationshipTO.Builder("grp_inclusion").otherEnd(HP_PRINTER_KEY).build());

        // set user manager bellini
        groupCR.setuManager(BELLINI_KEY);

        GroupTO groupTO = createGroup(groupCR).getEntity();

        String groupKey = groupTO.getKey();

        // first update: change name and attributes
        String originalName = groupTO.getName();
        groupCR.getTypeExtensions().add(userTE);
        updateGroup(new GroupUR.Builder(groupKey).name(
                        new StringReplacePatchItem.Builder().value(originalName + "_upd").build())
                .plainAttrs(attrAddReplacePatch("originalName", originalName),
                        attrAddReplacePatch("icon", "anotherIcon"))
                .typeExtension(userTE)
                .build());

        // second update: change manager and type extensions replacing the USER with PRINTER one
        TypeExtensionTO printerTE = new TypeExtensionTO();
        printerTE.setAnyType("PRINTER");
        printerTE.getAuxClasses().add("minimal printer");
        groupTO = updateGroup(
                new GroupUR.Builder(groupKey).uManager(new StringReplacePatchItem.Builder().value(PUCCINI_KEY).build())
                        .typeExtensions(List.of(userTE, printerTE))
                        .build()).getEntity();

        // 1. search and test shadows
        PagedResult<ShadowTO<GroupTO>> shadows = JAVERS_AUDIT_GROUP_SERVICE.shadows(groupKey, 1, 25);
        assertEquals(3, shadows.getTotalCount());

        // 2. search changes by entity key
        List<ChangesByCommitTO> changes = JAVERS_AUDIT_GROUP_SERVICE.changes(groupKey, "admin", null, null, 1, 25);
        assertFalse(changes.isEmpty());
        // changes in uManager
        assertTrue(changes.stream()
                .anyMatch(pc -> pc.getChanges()
                        .getValueChanges()
                        .stream()
                        .anyMatch(vc -> "uManager".equals(vc.getField()) && vc.getOldValues().contains(BELLINI_KEY)
                                && vc.getNewValues().contains(PUCCINI_KEY))));
        // changes key must match the group key
        assertTrue(changes.stream()
                .allMatch(c -> c.getChanges()
                        .getValueChanges()
                        .stream()
                        .allMatch(vc -> vc.getEntityKey().contains(groupKey))));

        // changes in name
        String newName = groupTO.getName();
        assertTrue(changes.stream()
                .anyMatch(c -> c.getChanges()
                        .getValueChanges()
                        .stream()
                        .anyMatch(vc -> "name".equals(vc.getField()) && vc.getNewValues().contains(newName)
                                && vc.getOldValues().contains(originalName))));

        // changes in attributes
        assertTrue(changes.stream()
                .anyMatch(c -> c.getChanges()
                        .getValueChanges()
                        .stream()
                        .anyMatch(vc -> "plainAttrs[icon]".equals(vc.getField()) && vc.getNewValues()
                                .contains("anotherIcon") && vc.getOldValues().contains("anIcon"))));

        assertTrue(changes.stream()
                .anyMatch(c -> c.getChanges()
                        .getValueChanges()
                        .stream()
                        .anyMatch(vc -> "plainAttrs[originalName]".equals(vc.getField()) && vc.getNewValues()
                                .contains(originalName) && vc.getOldValues().isEmpty())));

        // changes in type extensions
        assertTrue(changes.stream()
                .anyMatch(c -> c.getChanges()
                        .getValueChanges()
                        .stream()
                        .anyMatch(vc -> "typeExtensions".equals(vc.getField()) && vc.getOldValues().isEmpty()
                                && vc.getNewValues()
                                .contains("typeExtensions[USER].auxClasses[csv,generic membership]"))));
        assertTrue(changes.stream()
                .anyMatch(c -> c.getChanges()
                        .getValueChanges()
                        .stream()
                        .anyMatch(vc -> "typeExtensions".equals(vc.getField()) && vc.getNewValues()
                                .contains("typeExtensions[PRINTER].auxClasses[minimal printer]") && vc.getOldValues()
                                .contains("typeExtensions[USER].auxClasses[csv,generic membership]"))));
    }

    @Test
    void crudAnyObjects() {
        GroupTO otherchild = GROUP_SERVICE.read("f779c0d4-633b-4be5-8f57-32eb478a3ca5");
        GroupTO artDirector = GROUP_SERVICE.read("ece66293-8f31-4a84-8e8d-23da36e70846");

        AnyObjectCR anyObjectCR = AnyObjectITCase.getSample("3rdfloor");
        anyObjectCR.getResources().add(RESOURCE_NAME_NOPROPAGATION);
        anyObjectCR.getAuxClasses().add("other");
        // memberships
        anyObjectCR.getMemberships()
                .add(new MembershipTO.Builder(otherchild.getKey()).plainAttrs(attr("ctype", "printerctype"),
                        attr("cool", "false")).build());
        // relationships
        anyObjectCR.getRelationships().add(new RelationshipTO.Builder("inclusion").otherEnd(HP_PRINTER_KEY).build());

        anyObjectCR.setgManager(otherchild.getKey());

        AnyObjectTO newPrinter = createAnyObject(anyObjectCR).getEntity();

        try {
            // 1. update any object name, attribute and resources removing NOPROPAGATION and adding NOPROPAGATION2
            String originalName = newPrinter.getName();
            String newName = "4rdfloor" + getUUIDString();
            String originalLocation = newPrinter.getPlainAttr("location").orElseThrow().getValues().get(0);
            updateAnyObject(new AnyObjectUR.Builder(newPrinter.getKey()).name(
                            new StringReplacePatchItem.Builder().value(newName).build())
                    .plainAttr(attrAddReplacePatch("location", "4th floor"))
                    .resources(new StringPatchItem.Builder().value(RESOURCE_NAME_NOPROPAGATION)
                            .operation(PatchOperation.DELETE)
                            .build(), new StringPatchItem.Builder().value(RESOURCE_NAME_NOPROPAGATION2).build())
                    .build());
            // 2. update manager setting bellini instead of otherGroup, memberships removing otherChild and adding
            // artDirector and relationships replacing the HP with the Canon printer
            updateAnyObject(new AnyObjectUR.Builder(newPrinter.getKey()).gManager(
                            new StringReplacePatchItem.Builder().value(artDirector.getKey()).build())
                    .memberships(new MembershipUR.Builder(otherchild.getKey()).operation(PatchOperation.DELETE).build(),
                            new MembershipUR.Builder(artDirector.getKey()).build())
                    .relationships(new RelationshipUR.Builder("inclusion").otherEnd(HP_PRINTER_KEY)
                            .operation(PatchOperation.DELETE)
                            .build(), new RelationshipUR.Builder("inclusion").otherEnd(CANON_PRINTER_KEY).build())
                    .build());

            // 1. search and test shadows
            PagedResult<ShadowTO<AnyObjectTO>> shadows =
                    JAVERS_AUDIT_ANY_OBJECT_SERVICE.shadows(newPrinter.getKey(), 1, 25);
            assertEquals(3, shadows.getTotalCount());

            // 2. search changes by entity key
            List<ChangesByCommitTO> changes =
                    JAVERS_AUDIT_ANY_OBJECT_SERVICE.changes(newPrinter.getKey(), "admin", null, null, 1, 25);
            assertFalse(changes.isEmpty());
            // changes in gManager and uManager
            assertTrue(changes.stream()
                    .anyMatch(pc -> pc.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "gManager".equals(vc.getField()) && vc.getOldValues()
                                    .contains(otherchild.getKey()) && vc.getNewValues()
                                    .contains(artDirector.getKey()))));
            // changes key must match the any object key
            assertTrue(changes.stream()
                    .allMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .allMatch(vc -> vc.getEntityKey().contains(newPrinter.getKey()))));

            // changes in name
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "name".equals(vc.getField()) && vc.getNewValues().contains(newName)
                                    && vc.getOldValues().contains(originalName))));

            // changes on resources
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "resources".equals(vc.getField()) && vc.getNewValues()
                                    .contains(RESOURCE_NAME_NOPROPAGATION2) && vc.getOldValues()
                                    .contains(RESOURCE_NAME_NOPROPAGATION))));

            // changes in attributes
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "plainAttrs[location]".equals(vc.getField()) && vc.getNewValues()
                                    .contains("4th floor") && vc.getOldValues().contains(originalLocation))));
            // changes in memberships
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "memberships".equals(vc.getField()) && vc.getOldValues()
                                    .contains(otherchild.getKey() + "," + otherchild.getName()) && vc.getNewValues()
                                    .contains(artDirector.getKey() + "," + artDirector.getName()))));

            // changes in relationships
            assertTrue(changes.stream()
                    .anyMatch(c -> c.getChanges()
                            .getValueChanges()
                            .stream()
                            .anyMatch(vc -> "relationships".equals(vc.getField()) && vc.getOldValues()
                                    .contains("inclusion,LEFT,PRINTER," + HP_PRINTER_KEY) && vc.getNewValues()
                                    .contains("inclusion,LEFT,PRINTER," + CANON_PRINTER_KEY))));
        } finally {
            // delete any object and generate a delete event
            ANY_OBJECT_SERVICE.delete(newPrinter.getKey());
        }
        PagedResult<ShadowTO<AnyObjectTO>> shadows =
                JAVERS_AUDIT_ANY_OBJECT_SERVICE.shadows(newPrinter.getKey(), 1, 25);
        assertEquals(4, shadows.getTotalCount());
        assertEquals(4, shadows.getResult().size());
        Optional<ShadowTO<AnyObjectTO>> shadowCommit4 = shadows.getResult()
                .stream()
                .filter(shadow -> "TERMINAL".equals(shadow.getType()) && shadow.getVersion() == 4L)
                .findFirst();
        assertTrue(shadowCommit4.isPresent());
        assertEquals("admin", shadowCommit4.get().getWho());

    }

}
