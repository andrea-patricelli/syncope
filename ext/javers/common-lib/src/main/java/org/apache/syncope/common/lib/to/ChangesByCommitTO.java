package org.apache.syncope.common.lib.to;

public class ChangesByCommitTO implements EntityTO {

    private String commitId;

    private ChangesTO changes;

    public static final class Builder {

        private ChangesByCommitTO instance;

        public Builder(final String key) {
            getInstance();
            instance.commitId = key;
        }

        private ChangesByCommitTO newInstance() {
            return new ChangesByCommitTO();
        }

        private ChangesByCommitTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public ChangesByCommitTO.Builder changes(final ChangesTO changes) {
            this.getInstance().setChanges(changes);
            return this;
        }

        public ChangesByCommitTO build() {
            return getInstance();
        }
    }

    @Override
    public String getKey() {
        return this.commitId;
    }

    @Override
    public void setKey(final String key) {
        this.commitId = key;
    }

    public ChangesTO getChanges() {
        return changes;
    }

    public void setChanges(final ChangesTO changes) {
        this.changes = changes;
    }
}
