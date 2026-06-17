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
package org.apache.syncope.core.persistence.javers;

import java.util.Optional;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.javers.core.graph.ObjectAccessHook;
import org.javers.core.graph.ObjectAccessProxy;

public class OpenJpaUnproxyObjectAccessHook<T> implements ObjectAccessHook<T> {

    @Override
    public Optional<ObjectAccessProxy<T>> createAccessor(final T entity) {
        if (entity instanceof PersistenceCapable) {
            ObjectAccessProxy<T> proxy = new ObjectAccessProxy(() -> entity, entity.getClass(),
                    ((PersistenceCapable) entity).pcFetchObjectId());
            return Optional.of(proxy);
        }
        return Optional.empty();
    }
}
