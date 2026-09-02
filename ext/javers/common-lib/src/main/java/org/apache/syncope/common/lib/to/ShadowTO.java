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
import java.util.HashMap;
import java.util.Map;

public class ShadowTO<T extends AnyTO> implements EntityTO {

    private String key;

    private String type;

    private Long version;

    private T anyTO;

    private String who;

    private OffsetDateTime when;

    private Map<String, String> additionalInfo = new HashMap<>();

    public static final class Builder<T extends AnyTO> {

        private ShadowTO<T> instance;

        public Builder(final String key) {
            getInstance();
            instance.key = key;
        }

        private ShadowTO<T> newInstance() {
            return new ShadowTO<>();
        }

        private ShadowTO<T> getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public Builder<T> type(final String type) {
            this.getInstance().setType(type);
            return this;
        }

        public Builder<T> version(final Long version) {
            this.getInstance().setVersion(version);
            return this;
        }

        public Builder<T> anyTO(final T anyTO) {
            this.getInstance().setAnyTO(anyTO);
            return this;
        }

        public Builder<T> who(final String who) {
            this.getInstance().setWho(who);
            return this;
        }

        public Builder<T> when(final OffsetDateTime when) {
            this.getInstance().setWhen(when);
            return this;
        }

        public Builder<T> additionalInfo(final Map<String, String> additionalInfo) {
            this.getInstance().addAdditionalInfo(additionalInfo);
            return this;
        }

        public ShadowTO<T> build() {
            return getInstance();
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public T getAnyTO() {
        return anyTO;
    }

    public void setAnyTO(final T anyTO) {
        this.anyTO = anyTO;
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
