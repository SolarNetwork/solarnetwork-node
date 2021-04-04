/* ==================================================================
 * MediaResourceMimeType.java - 20/10/2019 6:56:15 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.camera.motion;

import java.util.regex.Pattern;

/**
 * Enumeration of media types supported by the motion integration.
 * 
 * @author matt
 * @version 1.0
 */
public enum MediaResourceMimeType {

	/** JPEG image. */
	JPEG(".+\\.(?:jpg|jpeg)", "image/jpeg"),

	/** PNG image. */
	PNG(".+\\.png", "image/png");

	final Pattern pattern;
	final String mimeType;
	final boolean video;

	private MediaResourceMimeType(String pat, String mimeType) {
		this.pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
		this.mimeType = mimeType;
		this.video = false;
	}

	/**
	 * Get an enum value for a given filename.
	 * 
	 * @param filename
	 *        the filename to get an associated enum value for
	 * @return the matching enum value, or {@literal null} if not supported
	 */
	public static MediaResourceMimeType forFilename(String filename) {
		for ( MediaResourceMimeType type : MediaResourceMimeType.values() ) {
			if ( type.pattern.matcher(filename).matches() ) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Get the MIME type.
	 * 
	 * @return the MIME type
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Get the video flag.
	 * 
	 * @return {@literal true} if this type represents video, {@literal false}
	 *         for an image
	 */
	public boolean isVideo() {
		return video;
	}

}
