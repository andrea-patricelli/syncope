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

import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Group form.
 */
public class GroupModalPanel extends AbstractModalPanel {

    private static final long serialVersionUID = -1732493223434085205L;

    protected final Mode mode;

    protected final boolean createFlag;

    protected final GroupPanel groupPanel;

    protected GroupTO originalGroupTO;

    public GroupModalPanel(final BaseModal<?> modal, final GroupTO groupTO) {
        this(modal, groupTO, Mode.ADMIN);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GroupModalPanel(final BaseModal<?> modal, final GroupTO groupTO, final Mode mode) {

        super(modal);

        this.mode = mode;

        this.createFlag = groupTO.getKey() == 0;
        if (!createFlag) {
            originalGroupTO = SerializationUtils.clone(groupTO);
        }

        final Form<GroupTO> form = new Form<>("groupForm");
        form.setMultiPart(true);

        add(new Label("displayName", groupTO.getKey() == 0 ? "" : groupTO.getDisplayName()));

        form.setModel(new CompoundPropertyModel<>(groupTO));

        this.groupPanel = new GroupPanel.Builder("groupPanel").
                form(form).groupTO(groupTO).groupModalPageMode(mode).build();
        form.add(groupPanel);

        final AjaxButton submit = new IndicatingAjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    submitAction(target, form);
                } catch (Exception e) {
                    LOG.error("Failure managing groupTO {}", groupTO, e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    modal.getFeedbackPanel().refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                modal.getFeedbackPanel().refresh(target);
            }
        };
        form.add(submit);
        form.setDefaultButton(submit);
    }

    protected void submitAction(final AjaxRequestTarget target, final Form<?> form) {
        final GroupTO groupTO = (GroupTO) form.getDefaultModelObject();

        GroupTO result;
        if (createFlag) {
            result = groupRestClient.create(groupTO);
        } else {
            result = groupRestClient.update(originalGroupTO.getETagValue(), groupTO);
        }

        //setResponsePage(new ResultStatusModal.Builder(window, result).build());
    }
}
