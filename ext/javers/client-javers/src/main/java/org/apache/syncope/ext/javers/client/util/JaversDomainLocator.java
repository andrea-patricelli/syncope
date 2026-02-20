package org.apache.syncope.ext.javers.client.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;

public class JaversDomainLocator {

    private final ConfigurableApplicationContext ctx;

    public JaversDomainLocator(final ConfigurableApplicationContext ctx) {
        this.ctx = ctx;
    }

    public <T> T getBean(final String domain, final Class<T> type) {
        return ctx.getBean("javers" + StringUtils.capitalize(domain), type);
    }

}
