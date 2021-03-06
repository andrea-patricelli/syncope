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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.ResultStatusModalPage;
import org.apache.syncope.client.console.pages.GroupModalPage;
import org.apache.syncope.client.console.pages.StatusModalPage;
import org.apache.syncope.client.console.rest.AbstractSubjectRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class GroupSearchResultPanel extends AbstractSearchResultPanel {

    private static final long serialVersionUID = -1180593361914008764L;

    private final static String PAGEID = "Groups";

    public <T extends AbstractAttributableTO> GroupSearchResultPanel(final String id, final boolean filtered,
            final String fiql, final PageReference callerRef, final AbstractSubjectRestClient restClient) {

        super(id, filtered, fiql, callerRef, restClient);
        initResultTable();
    }

    @Override
    protected List<IColumn<AbstractAttributableTO, String>> getColumns() {
        final List<IColumn<AbstractAttributableTO, String>> columns = new ArrayList<>();

        for (String item : new String[] { "key", "name", "entitlements" }) {
            columns.add(new PropertyColumn<AbstractAttributableTO, String>(new ResourceModel(item, item), item, item));
        }

        columns.add(new AbstractColumn<AbstractAttributableTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<AbstractAttributableTO>> cellItem,
                    final String componentId, final IModel<AbstractAttributableTO> model) {

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, page.getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        statusmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new StatusModalPage<GroupTO>(
                                        page.getPageReference(), statusmodal, (GroupTO) model.getObject());
                            }
                        });

                        statusmodal.show(target);
                    }
                }, ActionLink.ActionType.MANAGE_RESOURCES, PAGEID);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new GroupModalPage(
                                        page.getPageReference(), editmodal, (GroupTO) model.getObject());
                            }
                        });

                        editmodal.show(target);
                    }
                }, ActionLink.ActionType.EDIT, PAGEID);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            final GroupTO groupTO = (GroupTO) restClient.
                                    delete(model.getObject().getETagValue(), model.getObject().getKey());

                            page.setModalResult(true);

                            editmodal.setPageCreator(new ModalWindow.PageCreator() {

                                private static final long serialVersionUID = -7834632442532690940L;

                                @Override
                                public Page createPage() {
                                    return new ResultStatusModalPage.Builder(editmodal, groupTO).build();
                                }
                            });

                            editmodal.show(target);
                        } catch (SyncopeClientException scce) {
                            error(getString(Constants.OPERATION_ERROR) + ": " + scce.getMessage());
                            feedbackPanel.refresh(target);
                        }
                    }
                }, ActionLink.ActionType.DELETE, PAGEID);

                cellItem.add(panel);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>singletonList(ActionLink.ActionType.DELETE);
    }

    @Override
    protected String getPageId() {
        return PAGEID;
    }
}
