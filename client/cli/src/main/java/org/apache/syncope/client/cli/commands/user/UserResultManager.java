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
package org.apache.syncope.client.cli.commands.user;

import java.util.List;
import java.util.Set;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;

public class UserResultManager extends CommonsResultManager {

    public void toView(final List<UserTO> userTOs) {
        for (final UserTO userTO : userTOs) {
            printUser(userTO);
        }
    }

    private void printUser(final UserTO userTO) {
        System.out.println(" > USER ID: " + userTO.getKey());
        System.out.println("    username: " + userTO.getUsername());
        System.out.println("    realm: " + userTO.getRealm());
        System.out.println("    status: " + userTO.getStatus());
        System.out.println("    RESOURCES: ");
        printResource(userTO.getResources());
        System.out.println("    ROLES: ");
        printRole(userTO.getRoles());
        System.out.println("    creation date: " + userTO.getCreationDate());
        System.out.println("    change password date: " + userTO.getChangePwdDate());
        System.out.println("    PLAIN ATTRIBUTES: ");
        printAttributes(userTO.getPlainAttrs());
        System.out.println("    DERIVED ATTRIBUTES: ");
        printAttributes(userTO.getDerAttrs());
        System.out.println("    VIRTUAL ATTRIBUTES: ");
        printAttributes(userTO.getVirAttrs());
        System.out.println("    creator: " + userTO.getCreator());
        System.out.println("    last modifier: " + userTO.getLastModifier());
        System.out.println("    token: " + userTO.getToken());
        System.out.println("    token expiration time: " + userTO.getTokenExpireTime());
        System.out.println("    last change: " + userTO.getLastChangeDate());
        System.out.println("    last login: " + userTO.getLastLoginDate());
        System.out.println("    failed logins: " + userTO.getFailedLogins());
        System.out.println("RELATIONSHIPS:");
        printRelationships(userTO.getRelationships());
        System.out.println("    security question id: " + userTO.getSecurityQuestion());
        System.out.println("    security question answer id: " + userTO.getSecurityAnswer());
        System.out.println("");
    }

    private void printResource(final Set<String> resources) {
        for (final String resource : resources) {
            System.out.println("       - " + resource);
        }
    }

    private void printRole(final List<Long> roles) {
        for (final Long role : roles) {
            System.out.println("       - " + role);
        }
    }

    private void printAttributes(final Set<AttrTO> derAttrTOs) {
        for (final AttrTO attrTO : derAttrTOs) {
            final StringBuilder attributeSentence = new StringBuilder();
            attributeSentence.append("       ")
                    .append(attrTO.getSchema())
                    .append(": ")
                    .append(attrTO.getValues());
            if (attrTO.isReadonly()) {
                attributeSentence.append(" - is readonly.");
            }
            System.out.println(attributeSentence);
        }
    }

    private void printPropagationStatus(final List<PropagationStatus> propagationStatuses) {
        for (final PropagationStatus propagationStatus : propagationStatuses) {
            System.out.println("       status: " + propagationStatus.getStatus());
            System.out.println("       resource: " + propagationStatus.getResource());
            System.out.println("       failure reason: " + propagationStatus.getFailureReason());
        }
    }

    private void printRelationships(final List<RelationshipTO> relationshipTOs) {
        for (final RelationshipTO relationshipTO : relationshipTOs) {
            System.out.println("       type: " + relationshipTO.getType());
        }
    }
}
