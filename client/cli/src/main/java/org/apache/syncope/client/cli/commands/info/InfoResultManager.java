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
package org.apache.syncope.client.cli.commands.info;

import java.util.List;
import org.apache.syncope.client.cli.commands.CommonsResultManager;

public class InfoResultManager extends CommonsResultManager {

    public void printVersion(final String version) {
        genericMessage(" - Syncope version: " + version);
    }

    public void printPwdResetAllowed(final boolean pwdResetAllowed) {
        genericMessage("Password reset allowed: " + pwdResetAllowed);
    }

    public void printPwdResetRequiringSecurityQuestions(final boolean pwdResetRequiringSecurityQuestions) {
        genericMessage("Password reset requiring security question: " + pwdResetRequiringSecurityQuestions);
    }

    public void printSelfRegistrationAllowed(final boolean selfRegAllowed) {
        genericMessage("Self registration allowed: " + selfRegAllowed);
    }

    public void printProvisioningManager(final String anyObjectProvisioningManager,
            final String getUserProvisioningManager,
            final String getGroupProvisioningManager) {
        genericMessage(
                "Any object provisioning manager class: " + anyObjectProvisioningManager,
                "User provisioning manager class: " + getUserProvisioningManager,
                "Group provisioning manager class: " + getGroupProvisioningManager);
    }

    public void printWorkflowAdapter(final String anyObjectWorkflowAdapter,
            final String userWorkflowAdapter,
            final String groupWorkflowAdapter) {
        genericMessage(
                "Any object workflow adapter class: " + anyObjectWorkflowAdapter,
                "User       workflow adapter class: " + userWorkflowAdapter,
                "Group      workflow adapter class: " + groupWorkflowAdapter);
    }

    public void printAccountRules(final List<String> rules) {
        for (final String accountRule : rules) {
            genericMessage("Account rule: " + accountRule);
        }
    }

    public void printConnidLocations(final List<String> locations) {
        for (final String location : locations) {
            genericMessage("ConnId location: " + location);
        }
    }

    public void printLogicActions(final List<String> actions) {
        for (final String action : actions) {
            genericMessage("Logic action: " + action);
        }
    }

    public void printMailTemplates(final List<String> mailTemplates) {
        for (final String template : mailTemplates) {
            genericMessage("Mail template: " + template);
        }
    }

    public void printMappingItemTransformers(final List<String> transformers) {
        for (final String tranformer : transformers) {
            genericMessage("Mapping item tranformer: " + tranformer);
        }
    }

    public void printPasswordRules(final List<String> rules) {
        for (final String rule : rules) {
            genericMessage("Password rule: " + rule);
        }
    }

    public void printCorrelationRules(final List<String> rules) {
        for (final String rule : rules) {
            genericMessage("Correlation rule: " + rule);
        }
    }

    public void printPropagationActions(final List<String> actions) {
        for (final String action : actions) {
            genericMessage("Propagation action: " + action);
        }
    }

    public void printPushActions(final List<String> actions) {
        for (final String action : actions) {
            genericMessage("Push action: " + action);
        }
    }

    public void printSyncActions(final List<String> actions) {
        for (final String action : actions) {
            genericMessage("Sync action: " + action);
        }
    }

    public void printCorrelationActions(final List<String> actions) {
        for (final String action : actions) {
            genericMessage("Push correlation rule: " + action);
        }
    }

    public void printReportlets(final List<String> reportlets) {
        for (final String reportlet : reportlets) {
            genericMessage("Reportlet: " + reportlet);
        }
    }

    public void printJobs(final List<String> jobs) {
        for (final String job : jobs) {
            genericMessage("Task job: " + job);
        }
    }

    public void printValidators(final List<String> validators) {
        for (final String validator : validators) {
            genericMessage("Validator: " + validator);
        }
    }

    public void printPasswordGenerator(final String passwordGenerator) {
        genericMessage("Password generator class: " + passwordGenerator);
    }
    
    public void printVirtualAttributeCacheClass(final String virAttrCache) {
        genericMessage("Virtual attribute cache class: " + virAttrCache);
    }
}
