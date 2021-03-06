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
package org.apache.syncope.client.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.syncope.client.cli.commands.install.InstallConfigFileTemplate;
import org.apache.syncope.client.cli.util.JasyptUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SyncopeServices {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeServices.class);

    private static String SYNCOPE_ADDRESS;

    public static <T> T get(final Class<T> claz) {
        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(InstallConfigFileTemplate.FILE_PATH));
        } catch (final IOException e) {
            LOG.error("Error opening properties file", e);
        }

        final String syncopeAdminPassword = JasyptUtils.getJasyptUtils().decrypt(properties.getProperty(
                "syncope.admin.password"));
        SYNCOPE_ADDRESS = properties.getProperty("syncope.rest.services");
        final SyncopeClient syncopeClient = new SyncopeClientFactoryBean()
                .setAddress(SYNCOPE_ADDRESS)
                .create(properties.getProperty("syncope.admin.user"), syncopeAdminPassword);

        LOG.debug("Creting service for {}", claz.getName());
        return syncopeClient.getService(claz);
    }

    public static String getAddress() {
        return SYNCOPE_ADDRESS;
    }

    private SyncopeServices() {
        // private constructor for static utility class
    }
}
