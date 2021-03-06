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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SecurityQuestionLogic extends AbstractTransactionalLogic<SecurityQuestionTO> {

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SecurityQuestionDataBinder binder;

    @PreAuthorize("isAuthenticated()")
    public List<SecurityQuestionTO> list() {
        return CollectionUtils.collect(securityQuestionDAO.findAll(),
                new Transformer<SecurityQuestion, SecurityQuestionTO>() {

                    @Override
                    public SecurityQuestionTO transform(final SecurityQuestion input) {
                        return binder.getSecurityQuestionTO(input);
                    }
                }, new ArrayList<SecurityQuestionTO>());
    }

    @PreAuthorize("isAuthenticated()")
    public SecurityQuestionTO read(final Long securityQuestionId) {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(securityQuestionId);
        if (securityQuestion == null) {
            LOG.error("Could not find security question '" + securityQuestionId + "'");

            throw new NotFoundException(String.valueOf(securityQuestionId));
        }

        return binder.getSecurityQuestionTO(securityQuestion);
    }

    @PreAuthorize("hasRole('" + Entitlement.SECURITY_QUESTION_CREATE + "')")
    public SecurityQuestionTO create(final SecurityQuestionTO securityQuestionTO) {
        return binder.getSecurityQuestionTO(securityQuestionDAO.save(binder.create(securityQuestionTO)));
    }

    @PreAuthorize("hasRole('" + Entitlement.SECURITY_QUESTION_UPDATE + "')")
    public SecurityQuestionTO update(final SecurityQuestionTO securityQuestionTO) {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(securityQuestionTO.getKey());
        if (securityQuestion == null) {
            LOG.error("Could not find security question '" + securityQuestionTO.getKey() + "'");

            throw new NotFoundException(String.valueOf(securityQuestionTO.getKey()));
        }

        binder.update(securityQuestion, securityQuestionTO);
        securityQuestion = securityQuestionDAO.save(securityQuestion);

        return binder.getSecurityQuestionTO(securityQuestion);
    }

    @PreAuthorize("hasRole('" + Entitlement.SECURITY_QUESTION_DELETE + "')")
    public SecurityQuestionTO delete(final Long securityQuestionId) {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(securityQuestionId);
        if (securityQuestion == null) {
            LOG.error("Could not find security question '" + securityQuestionId + "'");

            throw new NotFoundException(String.valueOf(securityQuestionId));
        }

        SecurityQuestionTO deleted = binder.getSecurityQuestionTO(securityQuestion);
        securityQuestionDAO.delete(securityQuestionId);
        return deleted;
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    public SecurityQuestionTO read(final String username) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }
        User user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (user.getSecurityQuestion() == null) {
            LOG.error("Could not find security question for user '" + username + "'");

            throw new NotFoundException("Security question for user " + username);
        }

        return binder.getSecurityQuestionTO(user.getSecurityQuestion());
    }

    @Override
    protected SecurityQuestionTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof SecurityQuestionTO) {
                    key = ((SecurityQuestionTO) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                return binder.getSecurityQuestionTO(securityQuestionDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
