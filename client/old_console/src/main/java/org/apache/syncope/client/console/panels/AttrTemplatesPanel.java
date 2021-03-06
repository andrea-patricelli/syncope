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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SelectChoiceRenderer;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.NonI18nPalette;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttrTemplatesPanel extends Panel {

    public enum Type {

        gPlainAttrTemplates,
        gDerAttrTemplates,
        gVirAttrTemplates,
        mPlainAttrTemplates,
        mDerAttrTemplates,
        mVirAttrTemplates;

    }

    private static final long serialVersionUID = 1016028222120619000L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    private final GroupTO groupTO;

    private final NonI18nPalette<String> rPlainAttrTemplates;

    private final NonI18nPalette<String> rDerAttrTemplates;

    private final NonI18nPalette<String> rVirAttrTemplates;

    public AttrTemplatesPanel(final String id, final GroupTO groupTO) {
        super(id);
        this.groupTO = groupTO;

        rPlainAttrTemplates = buildPalette(Type.gPlainAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.GROUP, SchemaType.PLAIN));
        this.add(rPlainAttrTemplates);
        rDerAttrTemplates = buildPalette(Type.gDerAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.GROUP, SchemaType.DERIVED));
        this.add(rDerAttrTemplates);
        rVirAttrTemplates = buildPalette(Type.gVirAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.GROUP, SchemaType.VIRTUAL));
        this.add(rVirAttrTemplates);

        this.add(buildPalette(Type.mPlainAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP, SchemaType.PLAIN)));
        this.add(buildPalette(Type.mDerAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP, SchemaType.DERIVED)));
        this.add(buildPalette(Type.mVirAttrTemplates,
                schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP, SchemaType.VIRTUAL)));
    }

    private NonI18nPalette<String> buildPalette(final Type type, final List<String> allSchemas) {
        if (allSchemas != null && !allSchemas.isEmpty()) {
            Collections.sort(allSchemas);
        }
        ListModel<String> availableSchemas = new ListModel<>(allSchemas);

        return new NonI18nPalette<String>(type.name(), new PropertyModel<List<String>>(groupTO, type.name()),
                availableSchemas, new SelectChoiceRenderer<String>(), 8, false, true) {

                    private static final long serialVersionUID = 2295567122085510330L;

                    @Override
                    protected Recorder<String> newRecorderComponent() {
                        final Recorder<String> recorder = super.newRecorderComponent();

                        switch (type) {
                            case gPlainAttrTemplates:
                            case gDerAttrTemplates:
                            case gVirAttrTemplates:
                                recorder.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                                    private static final long serialVersionUID = -1107858522700306810L;

                                    @Override
                                    protected void onUpdate(final AjaxRequestTarget target) {
                                        send(getPage(), Broadcast.BREADTH, new GroupAttrTemplatesChange(type, target));
                                    }
                                });
                                break;

                            default:
                        }

                        return recorder;
                    }
                };
    }

    public Collection<String> getSelected(final Type type) {
        Collection<String> selected;
        switch (type) {
            case gPlainAttrTemplates:
                selected = this.rPlainAttrTemplates.getModelCollection();
                break;

            case gDerAttrTemplates:
                selected = this.rDerAttrTemplates.getModelCollection();
                break;

            case gVirAttrTemplates:
                selected = this.rVirAttrTemplates.getModelCollection();
                break;

            default:
                selected = Collections.emptyList();
        }

        return selected;
    }

    public static class GroupAttrTemplatesChange {

        private final Type type;

        private final AjaxRequestTarget target;

        public GroupAttrTemplatesChange(final Type type, final AjaxRequestTarget target) {
            this.type = type;
            this.target = target;
        }

        public Type getType() {
            return type;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
