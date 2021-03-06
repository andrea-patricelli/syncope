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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "virtualSchema")
public class VirSchemaTO extends AbstractSchemaTO {

    private static final long serialVersionUID = -8198557479659701343L;

    private boolean readonly;

    private long provision;

    private String extAttrName;

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    public long getProvision() {
        return provision;
    }

    public void setProvision(final long provision) {
        this.provision = provision;
    }

    public String getExtAttrName() {
        return extAttrName;
    }

    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

}
