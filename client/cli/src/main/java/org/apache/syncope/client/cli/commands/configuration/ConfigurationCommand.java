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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "configuration")
public class ConfigurationCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "Usage: configuration [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --get \n"
            + "    --read \n"
            + "       Syntax: --read {CONF-NAME} {CONF-NAME} [...] \n"
            + "    --update \n"
            + "       Syntax: --update {CONF-NAME}={CONF-VALUE} {CONF-NAME}={CONF-VALUE} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {CONF-NAME} {CONF-NAME} [...]\n"
            + "    --export \n"
            + "       Syntax: --export {WHERE-DIR}";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        switch (Options.fromName(input.getOption())) {
            case GET:
                new ConfigurationGet(input).get();
                break;
            case READ:
                new ConfigurationRead(input).read();
                break;
            case UPDATE:
                new ConfigurationUpdate(input).update();
                break;
            case DELETE:
                new ConfigurationDelete(input).delete();
                break;
            case EXPORT:
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new ConfigurationResultManager().defaultError(input.getOption(), HELP_MESSAGE);
                break;
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum Options {

        HELP("--help"),
        GET("--get"),
        READ("--read"),
        UPDATE("--update"),
        DELETE("--delete"),
        EXPORT("--export");

        private final String optionName;

        Options(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static Options fromName(final String name) {
            Options optionToReturn = HELP;
            for (final Options option : Options.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final Options value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
