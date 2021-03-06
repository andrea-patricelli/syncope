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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RealmModalPanel extends AbstractModalPanel {

    private static final long serialVersionUID = -4285220460543213901L;

    protected RealmTO realmTO;

    private final PageReference pageRef;

    private boolean newRealm = false;

    @SpringBean
    private RealmRestClient realmRestClient;

    private final String parentPath;

    public RealmModalPanel(
            final BaseModal<?> modal,
            final PageReference pageRef,
            final RealmTO realmTO,
            final String parentPath,
            final String entitlement) {
        this(modal, pageRef, realmTO, parentPath, entitlement, false);
    }

    public RealmModalPanel(
            final BaseModal<?> modal,
            final PageReference pageRef,
            final RealmTO realmTO,
            final String parentPath,
            final String entitlement,
            final boolean newRealm) {

        super(modal);
        this.newRealm = newRealm;

        this.pageRef = pageRef;
        this.realmTO = realmTO;
        this.parentPath = parentPath;

        final RealmDetails realmDetail = new RealmDetails("details", realmTO);
        if (SyncopeConsoleSession.get().owns(entitlement)) {
            MetaDataRoleAuthorizationStrategy.authorize(realmDetail, ENABLE, entitlement);
        }

        add(realmDetail);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            final RealmTO updatedRealmTO = RealmTO.class.cast(form.getModelObject());
            if (newRealm) {
                realmRestClient.create(this.parentPath, updatedRealmTO);
            } else {
                realmRestClient.update(updatedRealmTO);
            }

            if (pageRef.getPage() instanceof BasePage) {
                ((BasePage) pageRef.getPage()).setModalResult(true);
            }

            AbstractBasePage.class.cast(pageRef.getPage()).setModalResult(true);

            closeAction(target, form);
        } catch (Exception e) {
            LOG.error("While creating or updating realm", e);
            error(getString(Constants.ERROR) + ": " + e.getMessage());
            modal.getFeedbackPanel().refresh(target);
        }
    }
}
