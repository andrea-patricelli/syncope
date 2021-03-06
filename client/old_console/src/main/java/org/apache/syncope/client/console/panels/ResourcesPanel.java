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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.GroupTreeBuilder;
import org.apache.syncope.client.console.commons.SelectChoiceRenderer;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.NonI18nPalette;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ResourcesPanel extends Panel {

    private static final long serialVersionUID = -8728071019777410008L;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private GroupTreeBuilder groupTreeBuilder;

    private final AbstractSubjectTO subjectTO;

    private final Set<String> previousResources;

    private final List<String> allResources;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 8644108944633025494L;

        private String id;

        private Object to;

        private StatusPanel statusPanel;

        public Builder(final String id) {
            this.id = id;
        }

        public Builder attributableTO(final Object to) {
            this.to = to;
            return this;
        }

        public Builder statusPanel(final StatusPanel statusPanel) {
            this.statusPanel = statusPanel;
            return this;
        }

        public ResourcesPanel build() {
            return new ResourcesPanel(this);
        }
    }

    private ResourcesPanel(final Builder builder) {
        super(builder.id);
        subjectTO = (AbstractSubjectTO) builder.to;
        previousResources = new HashSet<>(subjectTO.getResources());
        allResources = new ArrayList<>();
        for (ResourceTO resourceTO : resourceRestClient.getAll()) {
            allResources.add(resourceTO.getKey());
        }
        Collections.sort(allResources);

        AjaxPalettePanel<String> resourcesPalette = null;

        if (subjectTO instanceof UserTO) {
            resourcesPalette = new AjaxRecordingPalettePanel<>("resourcesPalette",
                    new PropertyModel<List<String>>(subjectTO, "resources"),
                    new ListModel<>(allResources), builder.statusPanel);
        } else if (subjectTO instanceof GroupTO) {
            resourcesPalette = new AjaxPalettePanel<>("resourcesPalette",
                    new PropertyModel<List<String>>(subjectTO, "resources"), new ListModel<>(allResources));
        }
        add(resourcesPalette);
    }

    private class AjaxRecordingPalettePanel<T> extends AjaxPalettePanel<T> {

        private static final long serialVersionUID = -4215625881756021988L;

        private final StatusPanel statusPanel;

        public AjaxRecordingPalettePanel(final String id, final IModel<List<T>> model, final ListModel<T> choices,
                final StatusPanel statusPanel) {

            super(id, model, choices, new SelectChoiceRenderer<T>(), false, false);
            this.statusPanel = statusPanel;
        }

        @Override
        protected Palette<T> createPalette(final IModel<List<T>> model, final ListModel<T> choices,
                final IChoiceRenderer<T> renderer, final boolean allowOrder, final boolean allowMoveAll) {

            return new NonI18nPalette<T>("paletteField", model, choices, renderer, 8, allowOrder, false) {

                private static final long serialVersionUID = -3415146226879212841L;

                @Override
                protected Recorder<T> newRecorderComponent() {
                    Recorder<T> recorder = super.newRecorderComponent();
                    recorder.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = 5538299138211283825L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            if (subjectTO instanceof UserTO) {
                                UserTO userTO = (UserTO) subjectTO;

                                Set<String> resourcesToRemove = new HashSet<>(previousResources);
                                resourcesToRemove.removeAll(userTO.getResources());
                                if (!resourcesToRemove.isEmpty()) {
                                    Set<String> resourcesAssignedViaMembership = new HashSet<>();
                                    for (MembershipTO membTO : userTO.getMemberships()) {
                                        GroupTO groupTO = groupTreeBuilder.findGroup(membTO.getGroupKey());
                                        if (groupTO != null) {
                                            resourcesAssignedViaMembership.addAll(groupTO.getResources());
                                        }
                                    }
                                    resourcesToRemove.removeAll(resourcesAssignedViaMembership);
                                }

                                previousResources.clear();
                                previousResources.addAll(userTO.getResources());

                                StatusUtils.update(
                                        userTO, statusPanel, target, userTO.getResources(), resourcesToRemove);
                            }
                        }
                    });
                    return recorder;
                }
            };
        }
    }
}
