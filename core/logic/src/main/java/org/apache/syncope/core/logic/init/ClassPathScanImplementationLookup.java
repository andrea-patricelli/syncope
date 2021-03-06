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

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AccountRuleConfClass;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.provisioning.api.sync.SyncCorrelationRule;
import org.apache.syncope.core.provisioning.java.sync.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.sync.SyncJobDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Cache class names for all implementations of Syncope interfaces found in classpath, for later usage.
 */
@Component
public class ClassPathScanImplementationLookup implements ImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationLookup.class);

    private Map<Type, Set<String>> classNames;

    private Map<Class<? extends ReportletConf>, Class<? extends Reportlet>> reportletClasses;

    private Map<Class<? extends AccountRuleConf>, Class<? extends AccountRule>> accountRuleClasses;

    private Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> passwordRuleClasses;

    @Override
    public Integer getPriority() {
        return 400;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        classNames = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            classNames.put(type, new HashSet<String>());
        }

        reportletClasses = new HashMap<>();
        accountRuleClasses = new HashMap<>();
        passwordRuleClasses = new HashMap<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Reportlet.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccountRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PasswordRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(MappingItemTransformer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SchedTaskJobDelegate.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(LogicActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PropagationActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SyncActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PushActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SyncCorrelationRule.class));
        // Remove once SYNCOPE-470 is done
        //scanner.addIncludeFilter(new AssignableTypeFilter(PushCorrelationRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(Validator.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(StringUtils.EMPTY)) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (Reportlet.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.REPORTLET).add(clazz.getName());

                    ReportletConfClass annotation = clazz.getAnnotation(ReportletConfClass.class);
                    if (annotation != null) {
                        reportletClasses.put(annotation.value(), (Class<? extends Reportlet>) clazz);
                    }
                }

                if (AccountRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.ACCOUNT_RULE).add(clazz.getName());

                    AccountRuleConfClass annotation = clazz.getAnnotation(AccountRuleConfClass.class);
                    if (annotation != null) {
                        accountRuleClasses.put(annotation.value(), (Class<? extends AccountRule>) clazz);
                    }
                }
                if (PasswordRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PASSWORD_RULE).add(clazz.getName());
                    PasswordRuleConfClass annotation = clazz.getAnnotation(PasswordRuleConfClass.class);
                    if (annotation != null) {
                        passwordRuleClasses.put(annotation.value(), (Class<? extends PasswordRule>) clazz);
                    }
                }

                if (MappingItemTransformer.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.MAPPING_ITEM_TRANSFORMER).add(clazz.getName());
                }

                if (SchedTaskJobDelegate.class.isAssignableFrom(clazz) && !isAbsractClazz
                        && !SyncJobDelegate.class.isAssignableFrom(clazz)
                        && !PushJobDelegate.class.isAssignableFrom(clazz)) {

                    classNames.get(Type.TASKJOBDELEGATE).add(bd.getBeanClassName());
                }

                if (LogicActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.LOGIC_ACTIONS).add(bd.getBeanClassName());
                }

                if (PropagationActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PROPAGATION_ACTIONS).add(bd.getBeanClassName());
                }

                if (SyncActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.SYNC_ACTIONS).add(bd.getBeanClassName());
                }

                if (PushActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PUSH_ACTIONS).add(bd.getBeanClassName());
                }

                if (SyncCorrelationRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.SYNC_CORRELATION_RULE).add(bd.getBeanClassName());
                }

                // Uncomment when SYNCOPE-470 is done
                /* if (PushCorrelationRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                 * classNames.get(Type.PUSH_CORRELATION_RULES).add(metadata.getClassName());
                 * } */
                if (Validator.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.VALIDATOR).add(bd.getBeanClassName());
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }
        classNames = Collections.unmodifiableMap(classNames);
        reportletClasses = Collections.unmodifiableMap(reportletClasses);
        accountRuleClasses = Collections.unmodifiableMap(accountRuleClasses);
        passwordRuleClasses = Collections.unmodifiableMap(passwordRuleClasses);

        LOG.debug("Implementation classes found: {}", classNames);
    }

    @Override
    public Set<String> getClassNames(final Type type) {
        return classNames.get(type);
    }

    @Override
    public Class<? extends Reportlet> getReportletClass(
            final Class<? extends ReportletConf> reportletConfClass) {

        return reportletClasses.get(reportletConfClass);
    }

    @Override
    public Class<? extends AccountRule> getAccountRuleClass(
            final Class<? extends AccountRuleConf> accountRuleConfClass) {

        return accountRuleClasses.get(accountRuleConfClass);
    }

    @Override
    public Class<? extends PasswordRule> getPasswordRuleClass(
            final Class<? extends PasswordRuleConf> passwordRuleConfClass) {

        return passwordRuleClasses.get(passwordRuleConfClass);
    }

}
