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
package org.apache.syncope.client.console.pages;

import java.security.AccessControlException;
import java.util.Locale;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Login extends WebPage {

    private static final long serialVersionUID = 5889157642852559004L;

    @SpringBean(name = "anonymousUser")
    private String anonymousUser;

    private final NotificationPanel feedbackPanel;

    private final StatelessForm<Void> form;

    private final TextField<String> usernameField;

    private final TextField<String> passwordField;

    private final DropDownChoice<Locale> languageSelect;

    private final DropDownChoice<String> domainSelect;

    public Login(final PageParameters parameters) {
        super(parameters);
        setStatelessHint(true);

        feedbackPanel = new NotificationPanel(Constants.FEEDBACK);
        add(feedbackPanel);

        form = new StatelessForm<>("login");

        usernameField = new TextField<>("username", new Model<String>());
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model<String>());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        languageSelect = new LocaleDropDown("language");
        form.add(languageSelect);

        domainSelect = new DomainDropDown("domain");
        if (SyncopeConsoleSession.get().getDomains().size() == 1) {
            domainSelect.setOutputMarkupPlaceholderTag(true);
        }
        form.add(domainSelect);

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                if (anonymousUser.equals(usernameField.getRawInput())) {
                    throw new AccessControlException("Illegal username");
                }

                IAuthenticationStrategy strategy = getApplication().getSecuritySettings().getAuthenticationStrategy();

                if (AuthenticatedWebSession.get().signIn(usernameField.getRawInput(), passwordField.getRawInput())) {
                    // If login has been called because the user was not yet logged in, than continue to the
                    // original destination, otherwise to the Home page
                    continueToOriginalDestination();
                    setResponsePage(getApplication().getHomePage());
                } else {
                    error(getString("login-error"));
                    feedbackPanel.refresh(target);
                }
                strategy.remove();
            }
        };
        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);
        form.setDefaultButton(submitButton);

        add(form);
    }

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    private class LocaleDropDown extends DropDownChoice<Locale> {

        private static final long serialVersionUID = 2349382679992357202L;

        private class LocaleRenderer extends ChoiceRenderer<Locale> {

            private static final long serialVersionUID = -3657529581555164741L;

            @Override
            public String getDisplayValue(final Locale locale) {
                return locale.getDisplayName(getLocale());
            }
        }

        LocaleDropDown(final String id) {
            super(id, SyncopeConsoleApplication.SUPPORTED_LOCALES);

            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel<Locale>() {

                private static final long serialVersionUID = -6985170095629312963L;

                @Override
                public Locale getObject() {
                    return getSession().getLocale();
                }

                @Override
                public void setObject(final Locale object) {
                    getSession().setLocale(object);
                }

                @Override
                public void detach() {
                    // Empty.
                }
            });

            // set default value to English
            getModel().setObject(Locale.ENGLISH);
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications() {
            return true;
        }
    }

    /**
     * Inner class which implements (custom) Domain DropDownChoice component.
     */
    private class DomainDropDown extends DropDownChoice<String> {

        private static final long serialVersionUID = -7401167913360133325L;

        DomainDropDown(final String id) {
            super(id, SyncopeConsoleSession.get().getDomains());

            setModel(new IModel<String>() {

                private static final long serialVersionUID = -1124206668056084806L;

                @Override
                public String getObject() {
                    return SyncopeConsoleSession.get().getDomain();
                }

                @Override
                public void setObject(final String object) {
                    SyncopeConsoleSession.get().setDomain(object);
                }

                @Override
                public void detach() {
                    // Empty.
                }
            });

            // set default value to Master Domain
            getModel().setObject(SyncopeConstants.MASTER_DOMAIN);
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications() {
            return true;
        }
    }
}
