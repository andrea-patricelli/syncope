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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.PolicyType;

@XmlRootElement(name = "accountPolicy")
@XmlType
public class AccountPolicyTO extends AbstractPolicyTO {

    private static final long serialVersionUID = -1557150042828800134L;

    private boolean propagateSuspension;

    private int maxAuthenticationAttempts;

    private final List<AbstractAccountRuleConf> ruleConfs = new ArrayList<>();

    private final List<String> resources = new ArrayList<>();

    public AccountPolicyTO() {
        super(PolicyType.ACCOUNT);
    }

    public boolean isPropagateSuspension() {
        return propagateSuspension;
    }

    public void setPropagateSuspension(final boolean propagateSuspension) {
        this.propagateSuspension = propagateSuspension;
    }

    public int getMaxAuthenticationAttempts() {
        return maxAuthenticationAttempts;
    }

    public void setMaxAuthenticationAttempts(final int maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    @XmlElementWrapper(name = "ruleConfs")
    @XmlElement(name = "ruleConf")
    @JsonProperty("ruleConfs")
    public List<AbstractAccountRuleConf> getRuleConfs() {
        return ruleConfs;
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public List<String> getResources() {
        return resources;
    }
}
