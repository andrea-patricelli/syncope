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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.BaseBean;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyChangeTO implements BaseBean {

    private String entityKey;

    private String field;

    private String changeType;

    private List<String> oldValues = new ArrayList<>();

    private List<String> newValues = new ArrayList<>();

    public static final class Builder {

        private PropertyChangeTO instance;

        public Builder() {
            getInstance();
        }

        private PropertyChangeTO newInstance() {
            return new PropertyChangeTO();
        }

        private PropertyChangeTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public Builder entityKey(final String entityKey) {
            this.getInstance().setEntityKey(entityKey);
            return this;
        }

        public Builder property(final String property) {
            this.getInstance().setField(property);
            return this;
        }

        public Builder changeType(final String changeType) {
            this.getInstance().setChangeType(changeType);
            return this;
        }

        public Builder oldValues(final List<String> oldValues) {
            this.getInstance().getOldValues().addAll(oldValues);
            return this;
        }

        public Builder newValue(final List<String> newValues) {
            this.getInstance().getNewValues().addAll(newValues);
            return this;
        }

        public PropertyChangeTO build() {
            return getInstance();
        }
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(final String changeType) {
        this.changeType = changeType;
    }

    public List<String> getOldValues() {
        return oldValues;
    }

    public List<String> getNewValues() {
        return newValues;
    }
    
    public boolean isEmpty() {
        return oldValues.isEmpty() && newValues.isEmpty();
    }

    public enum PropertyChangeType {

        PROPERTY_ADDED,
        PROPERTY_REMOVED,
        PROPERTY_VALUE_CHANGED
    }
    
}
