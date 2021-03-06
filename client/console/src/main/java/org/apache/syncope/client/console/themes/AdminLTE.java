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
package org.apache.syncope.client.console.themes;

import de.agilecoders.wicket.core.settings.Theme;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.jqueryui.JQueryUIAllJavaScriptReference;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.jqueryui.JQueryUICoreJavaScriptReference;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.jqueryui.JQueryUIDraggableJavaScriptReference;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.jqueryui.JQueryUIMouseJavaScriptReference;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.jqueryui.JQueryUIResizableJavaScriptReference;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.jqueryui.JQueryUIWidgetJavaScriptReference;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.resource.JQueryPluginResourceReference;

public class AdminLTE extends Theme {

    public AdminLTE() {
        super("adminLTE");
    }

    @Override
    public List<HeaderItem> getDependencies() {
        final List<HeaderItem> references = new ArrayList<>();
        references.add(JavaScriptHeaderItem.forReference(
                new JQueryPluginResourceReference(AdminLTE.class, "js/AdminLTE-app.min.js"), "adminltejs"));

        // Adds WebjarsJavaScriptResourceReference about JQuery
        references.add(JavaScriptHeaderItem.forReference(JQueryUIAllJavaScriptReference.instance()));
        references.add(JavaScriptHeaderItem.forReference(JQueryUICoreJavaScriptReference.instance()));
        references.add(JavaScriptHeaderItem.forReference(JQueryUIMouseJavaScriptReference.instance()));
        references.add(JavaScriptHeaderItem.forReference(JQueryUIWidgetJavaScriptReference.instance()));
        references.add(JavaScriptHeaderItem.forReference(JQueryUIResizableJavaScriptReference.instance()));
        references.add(JavaScriptHeaderItem.forReference(JQueryUIDraggableJavaScriptReference.instance()));

        references.add(CssHeaderItem.forReference(AdminLTECssResourceReference.INSTANCE));
        references.addAll(super.getDependencies());
        return references;
    }
}
