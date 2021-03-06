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
package org.apache.syncope.fit.core.reference;

import org.apache.syncope.common.rest.api.service.SyncopeService;

public class CamelDetector {

    public static boolean isCamelEnabledForUsers(final SyncopeService syncopeService) {
        return syncopeService.info().getUserProvisioningManager().indexOf("Camel") != -1;
    }

    public static boolean isCamelEnabledForGroups(final SyncopeService syncopeService) {
        return syncopeService.info().getGroupProvisioningManager().indexOf("Camel") != -1;
    }

    public static boolean isCamelEnabledForAnyObjects(final SyncopeService syncopeService) {
        return syncopeService.info().getAnyObjectProvisioningManager().indexOf("Camel") != -1;
    }
}
