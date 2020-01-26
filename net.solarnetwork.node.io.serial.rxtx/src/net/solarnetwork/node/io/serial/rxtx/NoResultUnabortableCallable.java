/* ==================================================================
 * NoResultUnabortableCallable.java - Oct 27, 2014 11:18:25 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.rxtx;

/**
 * {@link UnabortableCallable} that returns no result.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class NoResultUnabortableCallable extends UnabortableCallable<Object> {

	@Override
	public Object call() throws Exception {
		doCall();
		return null;
	}

	/**
	 * Perform the task. This method is invoked by {@link #call()}.
	 * 
	 * @throws Exception
	 *         if any error occurrs
	 */
	protected abstract void doCall() throws Exception;

}
