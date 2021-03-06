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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.LoggerCategoryPanel;
import org.apache.syncope.client.console.panels.GroupSearchPanel;
import org.apache.syncope.client.console.panels.UserSearchPanel;
import org.apache.syncope.client.console.rest.LoggerRestClient;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;

class NotificationModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1975312550059578553L;

    @SpringBean
    private NotificationRestClient restClient;

    @SpringBean
    private LoggerRestClient loggerRestClient;

    public NotificationModalPage(final PageReference pageRef, final ModalWindow window,
            final NotificationTO notificationTO, final boolean createFlag) {

        final Form<NotificationTO> form =
                new Form<NotificationTO>(FORM, new CompoundPropertyModel<NotificationTO>(notificationTO));

        final AjaxTextFieldPanel sender = new AjaxTextFieldPanel("sender", getString("sender"),
                new PropertyModel<String>(notificationTO, "sender"));
        sender.addRequiredLabel();
        sender.addValidator(EmailAddressValidator.getInstance());
        form.add(sender);

        final AjaxTextFieldPanel subject = new AjaxTextFieldPanel("subject", getString("subject"),
                new PropertyModel<String>(notificationTO, "subject"));
        subject.addRequiredLabel();
        form.add(subject);

        final AjaxDropDownChoicePanel<String> template = new AjaxDropDownChoicePanel<String>(
                "template", getString("template"),
                new PropertyModel<String>(notificationTO, "template"));
        template.setChoices(confRestClient.getMailTemplates());
        template.addRequiredLabel();
        form.add(template);

        final AjaxDropDownChoicePanel<TraceLevel> traceLevel = new AjaxDropDownChoicePanel<TraceLevel>(
                "traceLevel", getString("traceLevel"),
                new PropertyModel<TraceLevel>(notificationTO, "traceLevel"));
        traceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        traceLevel.addRequiredLabel();
        form.add(traceLevel);

        final AjaxCheckBoxPanel isActive = new AjaxCheckBoxPanel("isActive",
                getString("isActive"), new PropertyModel<Boolean>(notificationTO, "active"));
        if (createFlag) {
            isActive.getField().setDefaultModelObject(Boolean.TRUE);
        }
        form.add(isActive);

        final WebMarkupContainer aboutContainer = new WebMarkupContainer("aboutContainer");
        aboutContainer.setOutputMarkupId(true);

        form.add(aboutContainer);

        final AjaxCheckBoxPanel checkAbout = new AjaxCheckBoxPanel("checkAbout", "checkAbout", new Model<Boolean>(
                notificationTO.getUserAbout() == null && notificationTO.getGroupAbout() == null));
        aboutContainer.add(checkAbout);

        final AjaxCheckBoxPanel checkUserAbout = new AjaxCheckBoxPanel("checkUserAbout", "checkUserAbout",
                new Model<Boolean>(notificationTO.getUserAbout() != null));
        aboutContainer.add(checkUserAbout);

        final AjaxCheckBoxPanel checkGroupAbout = new AjaxCheckBoxPanel("checkGroupAbout", "checkGroupAbout",
                new Model<Boolean>(notificationTO.getGroupAbout() != null));
        aboutContainer.add(checkGroupAbout);

        final UserSearchPanel userAbout =
                new UserSearchPanel.Builder("userAbout").fiql(notificationTO.getUserAbout()).build();
        aboutContainer.add(userAbout);
        userAbout.setEnabled(checkUserAbout.getModelObject());

        final GroupSearchPanel groupAbout =
                new GroupSearchPanel.Builder("groupAbout").fiql(notificationTO.getGroupAbout()).build();
        aboutContainer.add(groupAbout);
        groupAbout.setEnabled(checkGroupAbout.getModelObject());

        checkAbout.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (checkAbout.getModelObject()) {
                    checkUserAbout.setModelObject(Boolean.FALSE);
                    checkGroupAbout.setModelObject(Boolean.FALSE);
                    userAbout.setEnabled(Boolean.FALSE);
                    groupAbout.setEnabled(Boolean.FALSE);
                } else {
                    checkAbout.setModelObject(Boolean.TRUE);
                }
                target.add(aboutContainer);
            }
        });

        checkUserAbout.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (checkUserAbout.getModelObject()) {
                    checkAbout.setModelObject(!checkUserAbout.getModelObject());
                    checkGroupAbout.setModelObject(!checkUserAbout.getModelObject());
                    groupAbout.setEnabled(Boolean.FALSE);
                } else {
                    checkUserAbout.setModelObject(Boolean.TRUE);
                }
                userAbout.setEnabled(Boolean.TRUE);
                target.add(aboutContainer);
            }
        });

        checkGroupAbout.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (checkGroupAbout.getModelObject()) {
                    checkAbout.setModelObject(Boolean.FALSE);
                    checkUserAbout.setModelObject(Boolean.FALSE);
                    userAbout.setEnabled(Boolean.FALSE);
                } else {
                    checkGroupAbout.setModelObject(Boolean.TRUE);
                }
                groupAbout.setEnabled(Boolean.TRUE);
                target.add(aboutContainer);
            }
        });

        final AjaxDropDownChoicePanel<IntMappingType> recipientAttrType = new AjaxDropDownChoicePanel<IntMappingType>(
                "recipientAttrType", new ResourceModel("recipientAttrType", "recipientAttrType").getObject(),
                new PropertyModel<IntMappingType>(notificationTO, "recipientAttrType"));
        recipientAttrType.setChoices(new ArrayList<IntMappingType>(
                IntMappingType.getAttributeTypes(AttributableType.USER,
                        EnumSet.of(IntMappingType.UserId, IntMappingType.Password))));
        recipientAttrType.addRequiredLabel();
        form.add(recipientAttrType);

        final AjaxDropDownChoicePanel<String> recipientAttrName = new AjaxDropDownChoicePanel<String>(
                "recipientAttrName", new ResourceModel("recipientAttrName", "recipientAttrName").getObject(),
                new PropertyModel<String>(notificationTO, "recipientAttrName"));
        recipientAttrName.setChoices(getSchemaNames(recipientAttrType.getModelObject()));
        recipientAttrName.addRequiredLabel();
        form.add(recipientAttrName);

        recipientAttrType.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                recipientAttrName.setChoices(getSchemaNames(recipientAttrType.getModelObject()));
                target.add(recipientAttrName);
            }
        });

        form.add(new LoggerCategoryPanel(
                "eventSelection",
                loggerRestClient.listEvents(),
                new PropertyModel<List<String>>(notificationTO, "events"),
                getPageReference(),
                "Notification") {

                    private static final long serialVersionUID = 6429053774964787735L;

                    @Override
                    protected String[] getListRoles() {
                        return new String[] {};
                    }

                    @Override
                    protected String[] getChangeRoles() {
                        return new String[] {};
                    }
                });

        final WebMarkupContainer recipientsContainer = new WebMarkupContainer("recipientsContainer");
        recipientsContainer.setOutputMarkupId(true);

        form.add(recipientsContainer);

        final AjaxCheckBoxPanel checkStaticRecipients = new AjaxCheckBoxPanel("checkStaticRecipients",
                "recipients", new Model<Boolean>(!notificationTO.getStaticRecipients().isEmpty()));
        form.add(checkStaticRecipients);

        if (createFlag) {
            checkStaticRecipients.getField().setDefaultModelObject(Boolean.FALSE);
        }

        final AjaxTextFieldPanel staticRecipientsFieldPanel =
                new AjaxTextFieldPanel("panel", "staticRecipients", new Model<String>(null));
        staticRecipientsFieldPanel.addValidator(EmailAddressValidator.getInstance());
        staticRecipientsFieldPanel.setRequired(checkStaticRecipients.getModelObject());

        if (notificationTO.getStaticRecipients().isEmpty()) {
            notificationTO.getStaticRecipients().add(null);
        }

        final MultiFieldPanel<String> staticRecipients = new MultiFieldPanel<String>("staticRecipients",
                new PropertyModel<List<String>>(notificationTO, "staticRecipients"), staticRecipientsFieldPanel);
        staticRecipients.setEnabled(checkStaticRecipients.getModelObject());
        form.add(staticRecipients);

        final AjaxCheckBoxPanel checkRecipients =
                new AjaxCheckBoxPanel("checkRecipients", "checkRecipients",
                        new Model<Boolean>(notificationTO.getRecipients() == null ? false : true));
        recipientsContainer.add(checkRecipients);

        if (createFlag) {
            checkRecipients.getField().setDefaultModelObject(Boolean.TRUE);
        }

        final UserSearchPanel recipients =
                new UserSearchPanel.Builder("recipients").fiql(notificationTO.getRecipients()).build();

        recipients.setEnabled(checkRecipients.getModelObject());
        recipientsContainer.add(recipients);

        final AjaxCheckBoxPanel selfAsRecipient = new AjaxCheckBoxPanel("selfAsRecipient",
                getString("selfAsRecipient"), new PropertyModel<Boolean>(notificationTO, "selfAsRecipient"));
        form.add(selfAsRecipient);

        if (createFlag) {
            selfAsRecipient.getField().setDefaultModelObject(Boolean.FALSE);
        }

        selfAsRecipient.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (!selfAsRecipient.getModelObject()
                        && !checkRecipients.getModelObject()
                        && !checkStaticRecipients.getModelObject()) {

                    checkRecipients.getField().setDefaultModelObject(Boolean.TRUE);
                    target.add(checkRecipients);
                    recipients.setEnabled(checkRecipients.getModelObject());
                    target.add(recipients);
                    target.add(recipientsContainer);
                }
            }
        });

        checkRecipients.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (!checkRecipients.getModelObject()
                        && !selfAsRecipient.getModelObject()
                        && !checkStaticRecipients.getModelObject()) {

                    checkStaticRecipients.getField().setDefaultModelObject(Boolean.TRUE);
                    target.add(checkStaticRecipients);
                    staticRecipients.setEnabled(Boolean.TRUE);
                    target.add(staticRecipients);
                    staticRecipientsFieldPanel.setRequired(Boolean.TRUE);
                    target.add(staticRecipientsFieldPanel);
                }
                recipients.setEnabled(checkRecipients.getModelObject());
                target.add(recipients);
                target.add(recipientsContainer);
            }
        });

        checkStaticRecipients.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (!checkStaticRecipients.getModelObject()
                        && !selfAsRecipient.getModelObject()
                        && !checkRecipients.getModelObject()) {
                    checkRecipients.getField().setDefaultModelObject(Boolean.TRUE);
                    checkRecipients.setEnabled(Boolean.TRUE);
                    target.add(checkRecipients);
                }
                staticRecipients.setEnabled(checkStaticRecipients.getModelObject());
                staticRecipientsFieldPanel.setRequired(checkStaticRecipients.getModelObject());
                recipients.setEnabled(checkRecipients.getModelObject());
                target.add(staticRecipientsFieldPanel);
                target.add(staticRecipients);
                target.add(recipients);
                target.add(recipientsContainer);
            }
        });

        AjaxButton submit = new IndicatingAjaxButton(APPLY, new Model<String>(getString(SUBMIT))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                notificationTO.setUserAbout(
                        !checkAbout.getModelObject() && checkUserAbout.getModelObject() ? userAbout.buildFIQL() : null);
                notificationTO.setGroupAbout(
                        !checkAbout.getModelObject()
                        && checkGroupAbout.getModelObject() ? groupAbout.buildFIQL() : null);
                notificationTO.setRecipients(checkRecipients.getModelObject() ? recipients.buildFIQL() : null);
                notificationTO.getStaticRecipients().removeAll(Collections.singleton(null));

                try {
                    if (createFlag) {
                        restClient.create(notificationTO);
                    } else {
                        restClient.update(notificationTO);
                    }
                    info(getString(Constants.OPERATION_SUCCEEDED));

                    Configuration callerPage = (Configuration) pageRef.getPage();
                    callerPage.setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientException scee) {
                    error(getString(Constants.ERROR) + ": " + scee.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);

        String allowedRoles = createFlag
                ? xmlRolesReader.getEntitlement("Notification", "create")
                : xmlRolesReader.getEntitlement("Notification", "update");
        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        form.add(submit);
        form.setDefaultButton(submit);

        form.add(cancel);

        add(form);
    }

    private List<String> getSchemaNames(final IntMappingType type) {
        final List<String> result;

        if (type == null) {
            result = Collections.<String>emptyList();
        } else {
            switch (type) {
                case UserPlainSchema:
                    result = schemaRestClient.getPlainSchemaNames(AttributableType.USER);
                    break;

                case UserDerivedSchema:
                    result = schemaRestClient.getDerSchemaNames(AttributableType.USER);
                    break;

                case UserVirtualSchema:
                    result = schemaRestClient.getVirSchemaNames(AttributableType.USER);
                    break;

                case Username:
                    result = Collections.singletonList("Username");
                    break;

                default:
                    result = Collections.<String>emptyList();
            }
        }

        return result;
    }
}
