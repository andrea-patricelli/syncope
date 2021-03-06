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
package org.apache.syncope.client.cli.commands.configuration;

import java.util.LinkedList;
import org.apache.syncope.client.cli.Input;

public class ConfigurationGet extends AbstractConfigurationCommand {

    private static final String GET_HELP_MESSAGE = "configuration --get";

    private final Input input;

    public ConfigurationGet(final Input input) {
        this.input = input;
    }

    public void get() {
        if (input.parameterNumber() == 0) {
            try {
                configurationResultManager.fromGet(new LinkedList<>(configurationSyncopeOperations.list()));
            } catch (final Exception ex) {
                configurationResultManager.genericError(ex.getMessage());
            }
        } else {
            configurationResultManager.unnecessaryParameters(input.listParameters(), GET_HELP_MESSAGE);
        }
    }
}
