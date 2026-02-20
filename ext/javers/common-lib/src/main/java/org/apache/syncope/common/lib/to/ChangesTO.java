package org.apache.syncope.common.lib.to;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;

public class ChangesTO implements BaseBean {

    private List<PropertyChangeTO> valueChanges = new ArrayList<>();

    private String who;

    private OffsetDateTime when;

    private Map<String, String> additionalInfo = new HashMap<>();

    public static final class Builder {

        private ChangesTO instance;

        public Builder(final String key) {
            getInstance();
        }

        private ChangesTO newInstance() {
            return new ChangesTO();
        }

        private ChangesTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public Builder who(final String who) {
            this.getInstance().setWho(who);
            return this;
        }

        public Builder when(final OffsetDateTime when) {
            this.getInstance().setWhen(when);
            return this;
        }

        public Builder valueChanges(final List<PropertyChangeTO> valueChanges) {
            this.getInstance().getValueChanges().addAll(valueChanges);
            return this;
        }

        public Builder additionalInfo(final Map<String, String> additionalInfo) {
            this.getInstance().addAdditionalInfo(additionalInfo);
            return this;
        }

        public ChangesTO build() {
            return getInstance();
        }
    }

    public List<PropertyChangeTO> getValueChanges() {
        return valueChanges;
    }

    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    public OffsetDateTime getWhen() {
        return when;
    }

    public void setWhen(final OffsetDateTime when) {
        this.when = when;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void addAdditionalInfo(final Map<String, String> additionalInfo) {
        this.additionalInfo.putAll(additionalInfo);
    }
}
