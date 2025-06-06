/* ==================================================================
 * TestEmbeddedDatabaseFactoryBean.java - 11/04/2022 2:28:56 PM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Factory bean for an {@link TestEmbeddedDatabase} instance.
 *
 * @author matt
 * @version 1.1
 * @since 1.14
 */
public class TestEmbeddedDatabaseFactoryBean implements FactoryBean<TestEmbeddedDatabase> {

	private final Properties envProperties = new Properties();

	private final String environmentTypeKey;
	private TestEmbeddedDatabase db;

	/**
	 * Constructor.
	 *
	 * @param environmentTypeKey
	 *        the environment property key for the database type to use
	 */
	public TestEmbeddedDatabaseFactoryBean(String environmentTypeKey) {
		super();
		this.environmentTypeKey = environmentTypeKey;
	}

	public void loadEnvironmentProperties() {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("env.properties")) {
			envProperties.load(in);
		} catch ( IOException e ) {
			// we'll ignore this
		}
	}

	@Override
	public TestEmbeddedDatabase getObject() throws Exception {
		if ( db != null ) {
			return db;
		}
		loadEnvironmentProperties();
		EmbeddedDatabaseType dbType = dbType();
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().generateUniqueName(true)
				.setType(dbType);
		this.db = new SimpleTestEmbeddedDatabase(builder.build(), dbType);
		if ( dbType == EmbeddedDatabaseType.DERBY ) {
			TestDbUtils.setupDerbyFunctions(new JdbcTemplate(db));
		}
		return this.db;
	}

	@Override
	public Class<?> getObjectType() {
		return TestEmbeddedDatabase.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private EmbeddedDatabaseType dbType() {
		String type = envProperties.getProperty(environmentTypeKey, "derby");
		switch (type) {
			case "derby":
				return EmbeddedDatabaseType.DERBY;

			default:
				return EmbeddedDatabaseType.H2;
		}
	}

}
