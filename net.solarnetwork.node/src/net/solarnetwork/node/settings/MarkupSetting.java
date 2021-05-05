/* ==================================================================
 * MarkupSetting.java - 6/05/2021 7:01:09 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings;

/**
 * API for a setting that supports markup content, such as HTML or Markdown.
 * 
 * @author matt
 * @version 1.0
 * @since 1.82
 */
public interface MarkupSetting {

	/**
	 * Flag indicating the setting value contains markup such as HTML, Markdown,
	 * and so on.
	 * 
	 * @return {@literal true} if the setting value contains markup,
	 *         {@literal false} for plain text
	 */
	boolean isMarkup();

}
