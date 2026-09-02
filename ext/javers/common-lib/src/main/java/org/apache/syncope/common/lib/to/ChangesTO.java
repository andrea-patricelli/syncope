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
package org.apache.syncope.common.lib.to;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;

public class ChangesTO implements BaseBean {

    private List<PropertyChangeTO> valueChanges = new ArrayList<>();

    private String who;

    private OffsetDateTime when;

    private Map<String, String> additionalInfo = new HashMap<>();

    public static final class Builder {

        private ChangesTO instance;

        public Builder(final String key) {
            getInstance();
        }

        private ChangesTO newInstance() {
            return new ChangesTO();
        }

        private ChangesTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public Builder who(final String who) {
            this.getInstance().setWho(who);
            return this;
        }

        public Builder when(final OffsetDateTime when) {
            this.getInstance().setWhen(when);
            return this;
        }

        public Builder valueChanges(final List<PropertyChangeTO> valueChanges) {
            this.getInstance().getValueChanges().addAll(valueChanges);
            return this;
        }

        public Builder additionalInfo(final Map<String, String> additionalInfo) {
            this.getInstance().addAdditionalInfo(additionalInfo);
            return this;
        }

        public ChangesTO build() {
            return getInstance();
        }
    }

    public List<PropertyChangeTO> getValueChanges() {
        return valueChanges;
    }

    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    public OffsetDateTime getWhen() {
        return when;
    }

    public void setWhen(final OffsetDateTime when) {
        this.when = when;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void addAdditionalInfo(final Map<String, String> additionalInfo) {
        this.additionalInfo.putAll(additionalInfo);
    }
}
