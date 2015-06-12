
package net.solarnetwork.node.ocpp.test;

import net.solarnetwork.node.ocpp.dao.test.JdbcAuthorizationDaoTests;
import net.solarnetwork.node.ocpp.dao.test.JdbcChargeSessionDaoTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AuthorizationTests.class, JdbcAuthorizationDaoTests.class,
		JdbcChargeSessionDaoTests.class })
public class AllTests {

}
