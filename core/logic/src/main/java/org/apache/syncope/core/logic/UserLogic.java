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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class UserLogic extends AbstractAnyLogic<UserTO, UserPatch> {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    protected SyncopeLogic syncopeLogic;

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public String getUsername(final Long key) {
        return binder.getUserTO(key).getUsername();
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public Long getKey(final String username) {
        return binder.getUserTO(username).getKey();
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public int count(final List<String> realms) {
        return userDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_LIST), realms));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> list(
            final int page, final int size, final List<OrderByClause> orderBy,
            final List<String> realms, final boolean details) {

        return CollectionUtils.collect(userDAO.findAll(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_LIST), realms),
                page, size, orderBy),
                new Transformer<User, UserTO>() {

                    @Override
                    public UserTO transform(final User input) {
                        return binder.returnUserTO(binder.getUserTO(input, details));
                    }
                }, new ArrayList<UserTO>());
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Pair<String, UserTO> readSelf() {
        return ImmutablePair.of(
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                binder.returnUserTO(binder.getAuthenticatedUserTO()));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final Long key) {
        return binder.returnUserTO(binder.getUserTO(key));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public int searchCount(final SearchCond searchCondition, final List<String> realms) {
        return searchDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_SEARCH), realms),
                searchCondition, AnyTypeKind.USER);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final List<String> realms, final boolean details) {

        List<User> matchingUsers = searchDAO.search(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_SEARCH), realms),
                searchCondition, page, size, orderBy, AnyTypeKind.USER);
        return CollectionUtils.collect(matchingUsers, new Transformer<User, UserTO>() {

            @Override
            public UserTO transform(final User input) {
                return binder.returnUserTO(binder.getUserTO(input, details));
            }
        }, new ArrayList<UserTO>());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    public ProvisioningResult<UserTO> selfCreate(
            final UserTO userTO, final boolean storePassword, final boolean nullPriorityAsync) {

        return doCreate(userTO, storePassword, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_CREATE + "')")
    @Override
    public ProvisioningResult<UserTO> create(final UserTO userTO, final boolean nullPriorityAsync) {
        return doCreate(userTO, true, false, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_CREATE + "')")
    public ProvisioningResult<UserTO> create(
            final UserTO userTO, final boolean storePassword, final boolean nullPriorityAsync) {

        return doCreate(userTO, storePassword, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doCreate(
            final UserTO userTO,
            final boolean storePassword,
            final boolean self,
            final boolean nullPriorityAsync) {

        Pair<UserTO, List<LogicActions>> before = beforeCreate(userTO);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        if (!self) {
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_CREATE),
                    Collections.singleton(before.getLeft().getRealm()));
            securityChecks(effectiveRealms, before.getLeft().getRealm(), null);
        }

        Pair<Long, List<PropagationStatus>> created =
                provisioningManager.create(before.getLeft(), storePassword, nullPriorityAsync);

        return after(binder.returnUserTO(binder.getUserTO(created.getKey())), created.getRight(), before.getRight());
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    public ProvisioningResult<UserTO> selfUpdate(final UserPatch userPatch, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userPatch.setKey(userTO.getKey());
        return doUpdate(userPatch, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> update(final UserPatch userPatch, final boolean nullPriorityAsync) {
        return doUpdate(userPatch, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doUpdate(
            final UserPatch userPatch, final boolean self, final boolean nullPriorityAsync) {

        UserTO userTO = binder.getUserTO(userPatch.getKey());
        Pair<UserPatch, List<LogicActions>> before = beforeUpdate(userPatch, userTO.getRealm());

        if (!self
                && before.getLeft().getRealm() != null
                && StringUtils.isNotBlank(before.getLeft().getRealm().getValue())) {

            Set<String> requestedRealms = new HashSet<>();
            requestedRealms.add(before.getLeft().getRealm().getValue());
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                    requestedRealms);
            securityChecks(effectiveRealms, before.getLeft().getRealm().getValue(), before.getLeft().getKey());
        }

        Pair<Long, List<PropagationStatus>> updated = provisioningManager.update(before.getLeft(), nullPriorityAsync);

        return after(binder.returnUserTO(binder.getUserTO(updated.getKey())), updated.getRight(), before.getRight());
    }

    protected Pair<Long, List<PropagationStatus>> setStatusOnWfAdapter(
            final StatusPatch statusPatch, final boolean nullPriorityAsync) {

        Pair<Long, List<PropagationStatus>> updated;

        switch (statusPatch.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(statusPatch, nullPriorityAsync);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(statusPatch, nullPriorityAsync);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(statusPatch, nullPriorityAsync);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    public ProvisioningResult<UserTO> status(final StatusPatch statusPatch, final boolean nullPriorityAsync) {
        // security checks
        UserTO toUpdate = binder.getUserTO(statusPatch.getKey());
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(toUpdate.getRealm()));
        securityChecks(effectiveRealms, toUpdate.getRealm(), toUpdate.getKey());

        Pair<Long, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusPatch, nullPriorityAsync);

        return after(
                binder.returnUserTO(binder.getUserTO(updated.getKey())),
                updated.getRight(),
                Collections.<LogicActions>emptyList());
    }

    @PreAuthorize("hasRole('" + Entitlement.MUST_CHANGE_PASSWORD + "')")
    public ProvisioningResult<UserTO> changePassword(final String password, final boolean nullPriorityAsync) {
        UserPatch userPatch = new UserPatch();
        userPatch.setPassword(new PasswordPatch.Builder().value(password).build());
        userPatch.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(false).build());
        return selfUpdate(userPatch, nullPriorityAsync);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    @Transactional
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        User user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (syncopeLogic.isPwdResetRequiringSecurityQuestions()
                && (securityAnswer == null || !securityAnswer.equals(user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        provisioningManager.requestPasswordReset(user.getKey());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        User user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }
        provisioningManager.confirmPasswordReset(user.getKey(), token, password);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    public ProvisioningResult<UserTO> selfDelete(final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        return doDelete(userTO, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_DELETE + "')")
    @Override
    public ProvisioningResult<UserTO> delete(final Long key, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getUserTO(key);
        return doDelete(userTO, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doDelete(
            final UserTO userTO, final boolean self, final boolean nullPriorityAsync) {

        Pair<UserTO, List<LogicActions>> before = beforeDelete(userTO);

        if (!self) {
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_DELETE),
                    Collections.singleton(before.getLeft().getRealm()));
            securityChecks(effectiveRealms, before.getLeft().getRealm(), before.getLeft().getKey());
        }

        List<Group> ownedGroups = groupDAO.findOwnedByUser(before.getLeft().getKey());
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(CollectionUtils.collect(ownedGroups, new Transformer<Group, String>() {

                @Override
                public String transform(final Group group) {
                    return group.getKey() + " " + group.getName();
                }
            }, new ArrayList<String>()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(before.getLeft().getKey(), nullPriorityAsync);

        UserTO deletedTO;
        if (userDAO.find(before.getLeft().getKey()) == null) {
            deletedTO = new UserTO();
            deletedTO.setKey(before.getLeft().getKey());
        } else {
            deletedTO = binder.getUserTO(before.getLeft().getKey());
        }

        return after(binder.returnUserTO(deletedTO), statuses, before.getRight());
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build();
            }
        }));

        return binder.returnUserTO(binder.getUserTO(provisioningManager.unlink(patch)));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        return binder.returnUserTO(binder.getUserTO(provisioningManager.link(patch)));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> unassign(
            final Long key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build();
            }
        }));

        return update(patch, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> assign(
            final Long key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        if (changepwd) {
            patch.setPassword(new PasswordPatch.Builder().
                    value(password).onSyncope(false).resources(resources).build());
        }

        return update(patch, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> deprovision(
            final Long key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources, nullPriorityAsync);

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setAny(binder.returnUserTO(binder.getUserTO(key)));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> provision(
            final Long key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        List<PropagationStatus> statuses =
                provisioningManager.provision(key, changePwd, password, resources, nullPriorityAsync);

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setAny(binder.returnUserTO(binder.getUserTO(key)));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Object key = null;

        if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    key = ((UserTO) args[i]).getKey();
                } else if (args[i] instanceof UserPatch) {
                    key = ((UserPatch) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                return binder.returnUserTO(key instanceof Long
                        ? binder.getUserTO((Long) key) : binder.getUserTO((String) key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
