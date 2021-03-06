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

import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.panels.AnnotatedBeanPanel;
import org.apache.syncope.client.console.panels.DerAttrsPanel;
import org.apache.syncope.client.console.panels.PlainAttrsPanel;
import org.apache.syncope.client.console.panels.VirAttrsPanel;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

public class MembershipModalPage extends BaseModalPage {

    private static final long serialVersionUID = -4360802478081432549L;

    private final AjaxButton submit;

    public MembershipModalPage(final PageReference pageRef, final ModalWindow window, final MembershipTO membershipTO,
            final Mode mode) {

        final Form<MembershipTO> form = new Form<MembershipTO>("MembershipForm");

        final UserTO userTO = ((UserModalPage) pageRef.getPage()).getUserTO();

        form.setModel(new CompoundPropertyModel<MembershipTO>(membershipTO));

        submit = new AjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                userTO.getMemberships().remove(membershipTO);
                userTO.getMemberships().add(membershipTO);

                ((UserModalPage) pageRef.getPage()).setUserTO(userTO);

                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };

        form.add(submit);
        form.setDefaultButton(submit);

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                ((UserModalPage) pageRef.getPage()).setUserTO(userTO);
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        //--------------------------------
        // Attributes panel
        //--------------------------------
        form.add(new PlainAttrsPanel("plainAttrs", membershipTO, form, mode));
        form.add(new AnnotatedBeanPanel("systeminformation", membershipTO));
        //--------------------------------

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        form.add(new DerAttrsPanel("derAttrs", membershipTO));
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        form.add(new VirAttrsPanel("virAttrs", membershipTO, mode == Mode.TEMPLATE));
        //--------------------------------

        add(form);
    }
}
