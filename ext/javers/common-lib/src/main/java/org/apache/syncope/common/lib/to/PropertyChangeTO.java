package org.apache.syncope.common.lib.to;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.BaseBean;

public class PropertyChangeTO implements BaseBean {

    private String entityKey;

    private String field;

    private String changeType;

    private List<String> oldValues = new ArrayList<>();

    private List<String> newValues = new ArrayList<>();

    public static final class Builder {

        private PropertyChangeTO instance;

        public Builder() {
            getInstance();
        }

        private PropertyChangeTO newInstance() {
            return new PropertyChangeTO();
        }

        private PropertyChangeTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public Builder entityKey(final String entityKey) {
            this.getInstance().setEntityKey(entityKey);
            return this;
        }

        public Builder property(final String property) {
            this.getInstance().setField(property);
            return this;
        }

        public Builder changeType(final String changeType) {
            this.getInstance().setChangeType(changeType);
            return this;
        }

        public Builder oldValues(final List<String> oldValues) {
            this.getInstance().getOldValues().addAll(oldValues);
            return this;
        }

        public Builder newValue(final List<String> newValues) {
            this.getInstance().getNewValues().addAll(newValues);
            return this;
        }

        public PropertyChangeTO build() {
            return getInstance();
        }
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(final String changeType) {
        this.changeType = changeType;
    }

    public List<String> getOldValues() {
        return oldValues;
    }

    public List<String> getNewValues() {
        return newValues;
    }

}
