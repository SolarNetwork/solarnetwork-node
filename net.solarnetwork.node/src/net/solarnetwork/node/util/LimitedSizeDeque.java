/* ==================================================================
 * LimitedSizeDeque.java - 26/09/2017 2:09:22 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A non-blocking {@link Deque} with an enforced maximum number of elements.
 * 
 * <p>
 * Calls to {@link LimitedSizeDeque#addFirst(Object)} and
 * {@link #addLast(Object)} will remove elements to make room for the addition,
 * before adding the new element. The removal is done from the opposite size of
 * the deque.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.51
 */
public class LimitedSizeDeque<E> extends ArrayDeque<E> implements Deque<E> {

	private static final long serialVersionUID = -2004653495520273734L;

	private final int maximumSize;

	/**
	 * Constructor.
	 * 
	 * @param maximumSize
	 *        the maximum number of elements allowed
	 */
	public LimitedSizeDeque(int maximumSize) {
		super();
		this.maximumSize = maximumSize;
	}

	@Override
	public void addFirst(E e) {
		while ( size() >= maximumSize ) {
			removeLast();
		}
		super.addFirst(e);
	}

	@Override
	public void addLast(E e) {
		while ( size() >= maximumSize ) {
			removeFirst();
		}
		super.addLast(e);
	}

}
