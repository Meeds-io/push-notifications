package org.exoplatform.push.dao;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.push.domain.Device;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DeviceDaoTestIgnored {

  private static Connection conn;

  private static Liquibase liquibase;

  @Before
  public void setup() throws ClassNotFoundException, SQLException, LiquibaseException {
    Class.forName("org.hsqldb.jdbcDriver");
    conn = DriverManager.getConnection("jdbc:hsqldb:file:target/hsql-db", "sa", "");

    Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(conn));

    liquibase = new Liquibase("db/changelog/push-notifications.db.changelog-1.0.0.xml", new ClassLoaderResourceAccessor(), database);
    liquibase.update((String)null);

    PortalContainer container = PortalContainer.getInstance();

    //
    RequestLifeCycle.begin(container);

    EntityManagerService entityMgrService = (EntityManagerService) container.getComponentInstanceOfType(EntityManagerService.class);
    entityMgrService.getEntityManager().getTransaction().begin();
  }

  @Test
  public void test() {
    DeviceDao deviceDao = new DeviceDao();

    Device device = new Device();
    deviceDao.create(device);

    List<Device> devices = deviceDao.findAll();

    Assert.assertNotNull(devices);
    Assert.assertEquals(1, devices.size());
  }

  @After
  public void tearDown() throws SQLException, LiquibaseException {
    PortalContainer container = PortalContainer.getInstance();

    //
    EntityManagerService entityMgrService = (EntityManagerService) container.getComponentInstanceOfType(EntityManagerService.class);
    if (entityMgrService.getEntityManager() != null && entityMgrService.getEntityManager().getTransaction() != null
            && entityMgrService.getEntityManager().getTransaction().isActive()) {
      entityMgrService.getEntityManager().getTransaction().commit();
      //
      RequestLifeCycle.end();
    }

    liquibase.rollback(1000, null);
    conn.close();
  }
}