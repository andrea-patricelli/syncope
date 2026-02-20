package org.apache.syncope.core.persistence.javers.dao;

import org.apache.syncope.common.lib.to.ChangesTO;
import org.javers.core.changelog.ChangeProcessor;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ArrayChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ListChange;
import org.javers.core.diff.changetype.container.SetChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.javers.core.metamodel.object.GlobalId;

public class AnyChangeProcessor implements ChangeProcessor<ChangesTO> {
    
    public AnyChangeProcessor() {
    }

    @Override
    public void onCommit(final CommitMetadata commitMetadata) {
        
    }

    @Override
    public void onAffectedObject(final GlobalId globalId) {

    }

    @Override
    public void beforeChangeList() {

    }

    @Override
    public void afterChangeList() {

    }

    @Override
    public void beforeChange(final Change change) {

    }

    @Override
    public void afterChange(final Change change) {

    }

    @Override
    public void onPropertyChange(final PropertyChange propertyChange) {

    }

    @Override
    public void onValueChange(final ValueChange valueChange) {

    }

    @Override
    public void onReferenceChange(final ReferenceChange referenceChange) {

    }

    @Override
    public void onNewObject(final NewObject newObject) {

    }

    @Override
    public void onObjectRemoved(final ObjectRemoved objectRemoved) {

    }

    @Override
    public void onContainerChange(final ContainerChange containerChange) {

    }

    @Override
    public void onSetChange(final SetChange setChange) {

    }

    @Override
    public void onArrayChange(final ArrayChange arrayChange) {

    }

    @Override
    public void onListChange(final ListChange listChange) {

    }

    @Override
    public void onMapChange(final MapChange mapChange) {

    }

    @Override
    public ChangesTO result() {
        return null;
    }
}
