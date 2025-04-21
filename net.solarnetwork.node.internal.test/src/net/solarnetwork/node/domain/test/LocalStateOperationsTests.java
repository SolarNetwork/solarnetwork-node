/* ==================================================================
 * LocalStateOperationsTests.java - 16/04/2025 4:10:43 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.node.domain.LocalStateOperations;
import net.solarnetwork.node.domain.LocalStateType;

/**
 * Test cases for the {@link LocalStateOperations} interface.
 *
 * @author matt
 * @version 1.0
 */
public class LocalStateOperationsTests {

	private class TestLocalStateOperations implements LocalStateOperations {

		@Override
		public Object localState(String key, Object defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object saveLocalState(String key, LocalStateType type, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object saveLocalState(String key, LocalStateType type, Object value,
				Object expectedValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object getAndSaveLocalState(String key, LocalStateType type, Object value) {
			throw new UnsupportedOperationException();
		}

	}

	@Test
	public void changeLocalState_insert() {
		// GIVEN
		final String stateKey = UUID.randomUUID().toString();
		final LocalStateType stateType = LocalStateType.Int32;
		final Integer stateValue = 123;
		final LocalStateOperations ops = new TestLocalStateOperations() {

			@Override
			public Object getAndSaveLocalState(String key, LocalStateType type, Object value) {
				assertThat("Requested key is same as provided", key, is(equalTo(stateKey)));
				assertThat("Requested type is same as provided", type, is(equalTo(stateType)));
				assertThat("Requested value is same as provided", value, is(equalTo(stateValue)));
				return null;
			}

		};

		// WHEN
		boolean result = ops.changeLocalState(stateKey, stateType, 123);

		// THEN
		assertThat("State was changed because of insert", result, is(equalTo(true)));
	}

	@Test
	public void changeLocalState_same() {
		// GIVEN
		final String stateKey = UUID.randomUUID().toString();
		final LocalStateType stateType = LocalStateType.Int32;
		final Integer stateValue = 123;
		final LocalStateOperations ops = new TestLocalStateOperations() {

			@Override
			public Object getAndSaveLocalState(String key, LocalStateType type, Object value) {
				assertThat("Requested key is same as provided", key, is(equalTo(stateKey)));
				assertThat("Requested type is same as provided", type, is(equalTo(stateType)));
				assertThat("Requested value is same as provided", value, is(equalTo(stateValue)));
				return stateValue;
			}

		};

		// WHEN
		boolean result = ops.changeLocalState(stateKey, stateType, 123);

		// THEN
		assertThat("State was not changed because same as previous", result, is(equalTo(false)));
	}

	@Test
	public void changeLocalState_different() {
		// GIVEN
		final String stateKey = UUID.randomUUID().toString();
		final LocalStateType stateType = LocalStateType.Int32;
		final Integer stateValue = 123;
		final LocalStateOperations ops = new TestLocalStateOperations() {

			@Override
			public Object getAndSaveLocalState(String key, LocalStateType type, Object value) {
				assertThat("Requested key is same as provided", key, is(equalTo(stateKey)));
				assertThat("Requested type is same as provided", type, is(equalTo(stateType)));
				assertThat("Requested value is same as provided", value, is(equalTo(stateValue)));
				return stateValue - 1;
			}

		};

		// WHEN
		boolean result = ops.changeLocalState(stateKey, stateType, 123);

		// THEN
		assertThat("State was changed because different from previous", result, is(equalTo(true)));
	}

}
