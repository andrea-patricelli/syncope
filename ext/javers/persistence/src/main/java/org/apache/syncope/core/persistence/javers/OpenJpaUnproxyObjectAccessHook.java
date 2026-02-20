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
