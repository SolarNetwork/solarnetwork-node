/* ==================================================================
 * IteratorStatus.java - 16/11/2022 6:25:33 am
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

package net.solarnetwork.node.setup.web.support;

/**
 * An iterator status object, for use in views.
 * 
 * @param <T>
 *        the object type
 * @author matt
 * @version 1.0
 */
public class IteratorStatus<T> {

	private final int count;
	private T current;
	private int index;

	/**
	 * Create a new status object.
	 * 
	 * @param <T>
	 *        the object type
	 * @param count
	 *        the count
	 * @param index
	 *        the current index
	 * @param current
	 *        the current item
	 * @return the status instance
	 */
	public static <T> IteratorStatus<T> status(int count, int index, T current) {
		IteratorStatus<T> s = new IteratorStatus<>(count);
		s.setIndex(index);
		s.setCurrent(current);
		return s;
	}

	/**
	 * Constructor.
	 * 
	 * @param count
	 *        the count of items
	 */
	public IteratorStatus(int count) {
		super();
		this.count = count;
	}

	/**
	 * Get the current item.
	 * 
	 * @return the current
	 */
	public T getCurrent() {
		return current;
	}

	/**
	 * Set the current item.
	 * 
	 * @param current
	 *        the current item to set
	 */
	public void setCurrent(T current) {
		this.current = current;
	}

	/**
	 * Get the index.
	 * 
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Set the index.
	 * 
	 * @param index
	 *        the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Get the count.
	 * 
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Get the "first" flag.
	 * 
	 * @return {@literal true} if this is the first item
	 */
	public boolean isFirst() {
		return (count > 0 && index == 0);
	}

	/**
	 * Get the "last" flag.
	 * 
	 * @return {@literal true} if this is the last item
	 */
	public boolean isLast() {
		return (count > 0 && index + 1 == count);
	}

}
