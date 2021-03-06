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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AnyDisplayAttributesModalPage;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

public class AnySearchResultPanel extends AbstractSearchResultPanel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(AnySearchResultPanel.class);

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    protected final List<String> schemaNames;

    protected final List<String> dSchemaNames;

    protected final String pageID = "Any";
    
    private final String entitlement = "USER_LIST";

    public AnySearchResultPanel(final String type, final String parentId, final boolean filtered,
            final String fiql, final PageReference callerRef, final AbstractAnyRestClient restClient,
            final List<AnyTypeClassTO> anyTypeClassTOs, final String realm) {
        super(parentId, filtered, fiql, callerRef, restClient, realm, type);
        //setCustomMarkupId(markupId);
        add(new Label("name", type));

        this.schemaNames = new ArrayList<String>();
        for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
            this.schemaNames.addAll(anyTypeClassTO.getPlainSchemas());
        }
        this.dSchemaNames = new ArrayList<String>();
        for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
            this.dSchemaNames.addAll(anyTypeClassTO.getDerSchemas());
        }

        initResultTable();
    }

    @Override
    protected List<IColumn<AnyTO, String>> getColumns() {

        final List<IColumn<AnyTO, String>> columns =
                new ArrayList<IColumn<AnyTO, String>>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_ANY_DETAILS_VIEW)) {
            final Field field = ReflectionUtils.findField(AnyObjectTO.class, name);

            if ("token".equalsIgnoreCase(name)) {
                columns.add(new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(
                        new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_ANY_ATTRIBUTES_VIEW)) {
            if (schemaNames.contains(name)) {
                columns.add(new AttrColumn(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_ANY_DERIVED_ATTRIBUTES_VIEW)) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : AnyDisplayAttributesModalPage.ANY_DEFAULT_SELECTION) {
                columns.add(
                        new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            }

        }

        columns.add(new ActionColumn<AnyTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel getActions(final String componentId, final IModel<AnyTO> model) {

                final ActionLinksPanel.Builder<AnyTO> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<AnyTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyTO anyTO) {
                        editmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                // SYNCOPE-294: re-read anyTO before edit
                                AnyObjectTO anyTO = ((AnyObjectRestClient) restClient).read(model.getObject().getKey());
                                return null;
                            }
                        });

                        editmodal.show(target);
                    }
                }, ActionLink.ActionType.EDIT, entitlement).add(new ActionLink<AnyTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyTO anyTO) {
                        try {
                            AnyTO deleteAnyTO =
                                    restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());

                            page.setModalResult(true);

                            editmodal.setPageCreator(new ModalWindow.PageCreator() {

                                private static final long serialVersionUID = -7834632442532690940L;

                                @Override
                                public Page createPage() {
                                    return null;
                                }
                            });

                            editmodal.show(target);
                        } catch (SyncopeClientException scce) {
                            error(getString(Constants.OPERATION_ERROR) + ": " + scce.getMessage());
                            feedbackPanel.refresh(target);
                        }
                    }
                }, ActionLink.ActionType.DELETE, entitlement);

                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        displaymodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new AnyDisplayAttributesModalPage(
                                        page.getPageReference(), displaymodal, schemaNames, dSchemaNames);
                            }
                        });

                        displaymodal.show(target);
                    }
                }, ActionLink.ActionType.CHANGE_VIEW, entitlement).add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, entitlement);

                return panel.build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected <T extends AnyTO> Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionLink.ActionType> bulkActions = new ArrayList<ActionLink.ActionType>();

        bulkActions.add(ActionLink.ActionType.DELETE);
        bulkActions.add(ActionLink.ActionType.SUSPEND);
        bulkActions.add(ActionLink.ActionType.REACTIVATE);

        return bulkActions;
    }

    @Override
    protected String getPageId() {
        return pageID;
    }
}
