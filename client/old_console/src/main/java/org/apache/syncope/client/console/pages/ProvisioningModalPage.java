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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.panels.ActionDataTablePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.AbstractWrappable;
import org.apache.syncope.common.lib.wrap.SubjectKey;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class ProvisioningModalPage<T extends AbstractAttributableTO> extends AbstractStatusModalPage {

    private static final long serialVersionUID = -4285220460543213901L;

    private static final int ROWS_PER_PAGE = 10;

    private final ResourceTO resourceTO;

    private final Class<? extends AbstractAttributableTO> typeRef;

    private final PageReference pageRef;

    private final ModalWindow window;

    private final StatusUtils statusUtils;

    public ProvisioningModalPage(
            final PageReference pageRef,
            final ModalWindow window,
            final ResourceTO resourceTO,
            final Class<T> typeRef) {

        super();

        this.pageRef = pageRef;
        this.window = window;
        this.resourceTO = resourceTO;
        this.typeRef = typeRef;

        statusUtils = new StatusUtils((UserTO.class.isAssignableFrom(typeRef) ? userRestClient : groupRestClient));

        add(new Label("displayName", StringUtils.EMPTY));

        final List<IColumn<StatusBean, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("key", this, null, "Attributable key"),
                "attributableKey", "attributableKey"));
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("name", this, null, "Attributable name"),
                "attributableName", "attributableName"));
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("resourceName", this, null, "Resource name"),
                "resourceName", "resourceName"));
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("accountLink", this, null, "Account link"),
                "accountLink", "accountLink"));
        columns.add(new AbstractColumn<StatusBean, String>(
                new StringResourceModel("status", this, null, "")) {

                    private static final long serialVersionUID = -3503023501954863131L;

                    @Override
                    public String getCssClass() {
                        return "action";
                    }

                    @Override
                    public void populateItem(
                            final Item<ICellPopulator<StatusBean>> cellItem,
                            final String componentId,
                            final IModel<StatusBean> model) {
                                cellItem.
                                add(statusUtils.getStatusImagePanel(componentId, model.getObject().getStatus()));
                            }
                });

        final ActionDataTablePanel<StatusBean, String> table = new ActionDataTablePanel<>(
                "resourceDatatable",
                columns,
                (ISortableDataProvider<StatusBean, String>) new StatusBeanProvider(),
                ROWS_PER_PAGE,
                pageRef);

        final String pageId = "Resources";

        table.addAction(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    bulkAssociationAction(target, ResourceDeassociationActionType.UNLINK, table, columns);
                } catch (Exception e) {
                    LOG.error("Error unlinkink resources", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        }, ActionLink.ActionType.UNLINK, pageId);

        table.addAction(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    bulkAssociationAction(target, ResourceDeassociationActionType.DEPROVISION, table, columns);
                } catch (Exception e) {
                    LOG.error("Error de-provisioning user", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        }, ActionLink.ActionType.DEPROVISION, pageId);

        table.addAction(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    bulkAssociationAction(target, ResourceDeassociationActionType.UNASSIGN, table, columns);
                } catch (Exception e) {
                    LOG.error("Error unassigning resources", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        }, ActionLink.ActionType.UNASSIGN, pageId);

        table.addCancelButton(window);

        add(table);
    }

    private class StatusBeanProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4287357360778016173L;

        public StatusBeanProvider() {
            super("accountLink");
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<StatusBean> getStatusBeans() {
            final String fiql = SyncopeClient.getUserSearchConditionBuilder().hasResources(resourceTO.getKey()).query();

            final List<T> subjects = new ArrayList<>();
            if (UserTO.class.isAssignableFrom(typeRef)) {
                subjects.addAll((List<T>) userRestClient.search(fiql, 1, ROWS_PER_PAGE, new SortParam<>("key", true)));
            } else {
                subjects.addAll((List<T>) groupRestClient.search(fiql, 1, ROWS_PER_PAGE, new SortParam<>("key", true)));
            }

            final List<ConnObjectWrapper> connObjects = statusUtils.getConnectorObjects(
                    (List<AbstractSubjectTO>) subjects, Collections.<String>singleton(resourceTO.getKey()));

            final List<StatusBean> statusBeans = new ArrayList<>(connObjects.size() + 1);
            final LinkedHashMap<String, StatusBean> initialStatusBeanMap = new LinkedHashMap<>(connObjects.size());

            for (ConnObjectWrapper entry : connObjects) {
                final StatusBean statusBean = statusUtils.getStatusBean(entry.getAttributable(),
                        entry.getResourceName(),
                        entry.getConnObjectTO(),
                        GroupTO.class.isAssignableFrom(typeRef));

                initialStatusBeanMap.put(entry.getResourceName(), statusBean);
                statusBeans.add(statusBean);
            }

            return statusBeans;
        }
    }

    private void bulkAssociationAction(
            final AjaxRequestTarget target,
            final ResourceDeassociationActionType type,
            final ActionDataTablePanel<StatusBean, String> table,
            final List<IColumn<StatusBean, String>> columns) {

        final List<StatusBean> beans = new ArrayList<>(table.getModelObject());
        List<SubjectKey> subjectKeys = new ArrayList<>();
        for (StatusBean bean : beans) {
            LOG.debug("Selected bean {}", bean);
            subjectKeys.add(AbstractWrappable.getInstance(SubjectKey.class, bean.getAttributableId()));
        }

        if (beans.isEmpty()) {
            window.close(target);
        } else {
            final BulkActionResult res = resourceRestClient.bulkAssociationAction(
                    resourceTO.getKey(), typeRef, type, subjectKeys);

            ((BasePage) pageRef.getPage()).setModalResult(true);

            setResponsePage(new BulkActionResultModalPage<>(window, beans, columns, res, "attributableKey"));
        }
    }
}
