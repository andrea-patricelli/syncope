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
package org.apache.syncope.core.logic.init;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-sensible (via {@code @Transactional} access to logger / audit data.
 *
 * @see LoggerLoader
 */
@Component
public class LoggerAccessor {

    @Autowired
    private LoggerDAO loggerDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Transactional
    public void synchronizeLog4J(final LoggerContext ctx) {
        Map<String, Logger> syncopeLoggers = new HashMap<>();
        if (SyncopeConstants.MASTER_DOMAIN.equals(AuthContextUtils.getDomain())) {
            for (Logger syncopeLogger : loggerDAO.findAll(LoggerType.LOG)) {
                syncopeLoggers.put(syncopeLogger.getKey(), syncopeLogger);
            }
        }
        for (Logger syncopeLogger : loggerDAO.findAll(LoggerType.AUDIT)) {
            syncopeLoggers.put(syncopeLogger.getKey(), syncopeLogger);
        }

        /*
         * Traverse all defined log4j loggers: if there is a matching SyncopeLogger, set log4j level accordingly,
         * otherwise create a SyncopeLogger instance with given name and level.
         */
        for (LoggerConfig logConf : ctx.getConfiguration().getLoggers().values()) {
            if (!LogManager.ROOT_LOGGER_NAME.equals(logConf.getName()) && logConf.getLevel() != null) {
                String loggerName = logConf.getName();
                if (syncopeLoggers.containsKey(loggerName)) {
                    logConf.setLevel(syncopeLoggers.get(loggerName).getLevel().getLevel());
                    syncopeLoggers.remove(loggerName);
                } else if (!loggerName.equals(LoggerType.AUDIT.getPrefix())) {
                    Logger syncopeLogger = entityFactory.newEntity(Logger.class);
                    syncopeLogger.setKey(loggerName);
                    syncopeLogger.setLevel(LoggerLevel.fromLevel(logConf.getLevel()));
                    syncopeLogger.setType(loggerName.startsWith(LoggerType.AUDIT.getPrefix())
                            ? LoggerType.AUDIT
                            : LoggerType.LOG);
                    loggerDAO.save(syncopeLogger);
                }
            }
        }

        /*
         * Foreach SyncopeLogger not found in log4j create a new log4j logger with given name and level.
         */
        for (Logger syncopeLogger : syncopeLoggers.values()) {
            LoggerConfig logConf = ctx.getConfiguration().getLoggerConfig(syncopeLogger.getKey());
            if (!LogManager.ROOT_LOGGER_NAME.equals(logConf.getName()) && logConf.getLevel() != null) {
                logConf.setLevel(syncopeLogger.getLevel().getLevel());
            }
        }
    }
}
