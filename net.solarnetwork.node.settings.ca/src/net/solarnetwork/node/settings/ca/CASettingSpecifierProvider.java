/* ==================================================================
 * CASettingSpecifierProvider.java - Mar 12, 2012 4:46:38 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings.ca;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * {@link SettingSpecifierProvider} with additional metadata to work with
 * ConfigurationAdmin.
 *
 * @author matt
 * @version 2.1
 */
public class CASettingSpecifierProvider implements SettingSpecifierProvider {

	private final String pid;
	private final SettingSpecifierProvider delegate;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate
	 * @param pid
	 *        the PID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CASettingSpecifierProvider(SettingSpecifierProvider delegate, String pid) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.pid = requireNonNullArgument(pid, "pid");
	}

	@Override
	public @Nullable MessageSource getMessageSource() {
		return delegate.getMessageSource();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return delegate.getSettingSpecifiers();
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return delegate.templateSettingSpecifiers();
	}

	@Override
	public String getSettingUid() {
		return (pid != null ? pid : delegate.getSettingUid());
	}

	@Override
	public @Nullable String getDisplayName() {
		return delegate.getDisplayName();
	}

	/**
	 * Get the PID.
	 *
	 * @return the PID
	 */
	public final String getPid() {
		return pid;
	}

	/**
	 * Get the delegate.
	 *
	 * @return the delegate
	 */
	public final SettingSpecifierProvider getDelegate() {
		return delegate;
	}

}
