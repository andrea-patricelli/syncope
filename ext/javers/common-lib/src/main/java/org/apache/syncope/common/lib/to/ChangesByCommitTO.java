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

public class ChangesByCommitTO implements EntityTO {

    private String commitId;

    private ChangesTO changes;

    public static final class Builder {

        private ChangesByCommitTO instance;

        public Builder(final String key) {
            getInstance();
            instance.commitId = key;
        }

        private ChangesByCommitTO newInstance() {
            return new ChangesByCommitTO();
        }

        private ChangesByCommitTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public ChangesByCommitTO.Builder changes(final ChangesTO changes) {
            this.getInstance().setChanges(changes);
            return this;
        }

        public ChangesByCommitTO build() {
            return getInstance();
        }
    }

    @Override
    public String getKey() {
        return this.commitId;
    }

    @Override
    public void setKey(final String key) {
        this.commitId = key;
    }

    public ChangesTO getChanges() {
        return changes;
    }

    public void setChanges(final ChangesTO changes) {
        this.changes = changes;
    }
}
