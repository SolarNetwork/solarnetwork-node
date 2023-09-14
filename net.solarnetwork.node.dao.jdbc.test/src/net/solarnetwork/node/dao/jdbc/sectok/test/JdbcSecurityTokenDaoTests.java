/* ==================================================================
 * JdbcSecurityTokenDaoTests.java - 6/09/2023 3:43:03 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.sectok.test;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.sectok.JdbcSecurityTokenDao;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.TestEmbeddedDatabase;

/**
 * Test cases for the {@link JdbcSecurityTokenDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcSecurityTokenDaoTests extends AbstractNodeTest {

	private TestEmbeddedDatabase dataSource;

	private JdbcSecurityTokenDao dao;
	private SecurityToken last;

	@Before
	public void setup() throws IOException {
		dao = new JdbcSecurityTokenDao();

		TestEmbeddedDatabase db = createEmbeddedDatabase("data.db.type");
		if ( db.getDatabaseType() != EmbeddedDatabaseType.DERBY ) {
			String dbType = db.getDatabaseType().toString().toLowerCase();
			dao.setInitSqlResource(new ClassPathResource(format("%s-sectok-init.sql", dbType),
					JdbcSecurityTokenDao.class));
			dao.setSqlResourcePrefix(format("%s-sectok", dbType));
		}
		dataSource = db;

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao.setDataSource(dataSource);
		dao.init();
	}

	private SecurityToken createTestSecurityToken(String tokenId) {
		return createTestSecurityToken(tokenId, randomUUID().toString().replace("-", ""),
				randomUUID().toString(), randomUUID().toString());
	}

	private SecurityToken createTestSecurityToken(String name, String description) {
		return createTestSecurityToken(UUID.randomUUID().toString().substring(0, 20),
				UUID.randomUUID().toString().replace("-", ""), name, description);
	}

	private SecurityToken createTestSecurityToken(String tokenId, String tokenSecret, String name,
			String description) {
		return new SecurityToken(tokenId, Instant.now().truncatedTo(ChronoUnit.MILLIS), tokenSecret,
				name, description);
	}

	@Test
	public void insert() {
		// GIVEN
		SecurityToken entity = createTestSecurityToken(UUID.randomUUID().toString(),
				UUID.randomUUID().toString());

		// WHEN
		String pk = dao.save(entity);

		// THEN
		assertThat("PK returned", pk, is(equalTo(entity.getId())));
		last = entity;
	}

	@Test
	public void getByPk() {
		// GIVEN
		insert();

		// WHEN
		SecurityToken entity = dao.get(last.getId());

		// THEN
		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Name", entity.getName(), is(equalTo(last.getName())));
		assertThat("Description", entity.getDescription(), is(equalTo(last.getDescription())));

		String[] tokenSecretHolder = new String[2];
		entity.copySecret(secret -> tokenSecretHolder[0] = secret);
		last.copySecret(secret -> tokenSecretHolder[1] = secret);
		assertThat("Secret", tokenSecretHolder[0], is(equalTo(tokenSecretHolder[1])));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		SecurityToken orig = dao.get(last.getId());
		SecurityToken update = orig.copyWithoutSecret(randomUUID().toString(), randomUUID().toString());
		String pk = dao.save(update);

		// THEN
		assertThat("PK unchanged", pk, equalTo(orig.getId()));

		SecurityToken entity = dao.get(pk);

		assertThat("Created unchanged", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Name updated", entity.getName(), is(equalTo(update.getName())));
		assertThat("Description updated", entity.getDescription(), is(equalTo(update.getDescription())));

		String[] tokenSecretHolder = new String[2];
		entity.copySecret(secret -> tokenSecretHolder[0] = secret);
		last.copySecret(secret -> tokenSecretHolder[1] = secret);
		assertThat("Secret unchanged", tokenSecretHolder[0], is(equalTo(tokenSecretHolder[1])));
	}

	@Test
	public void findAll() {
		// GIVEN
		SecurityToken obj1 = createTestSecurityToken("a");
		obj1 = dao.get(dao.save(obj1));
		SecurityToken obj2 = createTestSecurityToken("c");
		obj2 = dao.get(dao.save(obj2));
		SecurityToken obj3 = createTestSecurityToken("b");
		obj3 = dao.get(dao.save(obj3));

		// WHEN
		Collection<SecurityToken> results = dao.getAll(null);

		// THEN
		assertThat("Results found in order", results, contains(obj1, obj3, obj2));
		assertThat("All token secrets are null", results.stream().map(t -> {
			String[] holder = new String[1];
			t.copySecret(s -> holder[0] = s);
			return holder[0];
		}).collect(Collectors.toList()), contains((String) null, null, null));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		SecurityToken input = SecurityToken.tokenDetails(last.getId(), null, null);
		dao.delete(input);

		// THEN
		Collection<SecurityToken> all = dao.getAll(null);
		assertThat("Token deleted from table", all, hasSize(0));
	}

}
