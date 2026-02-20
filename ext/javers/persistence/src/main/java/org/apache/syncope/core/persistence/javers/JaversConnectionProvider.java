package org.apache.syncope.core.persistence.javers;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.javers.repository.sql.ConnectionProvider;

public class JaversConnectionProvider implements ConnectionProvider {

    protected EntityManager entityManager;

    public JaversConnectionProvider(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return (Connection) entityManager.unwrap(OpenJPAEntityManager.class).getConnection();
    }
}
