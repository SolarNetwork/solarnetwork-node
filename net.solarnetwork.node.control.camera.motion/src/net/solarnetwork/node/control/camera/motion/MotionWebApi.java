/* ==================================================================
 * MotionWebApi.java - 20/10/2019 7:05:05 pm
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

import java.util.Map;
import net.solarnetwork.io.UrlUtils;

/**
 * Methods for the motion web control API.
 * 
 * <p>
 * All methods are relative to the motion base URL and leading camera ID path
 * segment.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public enum MotionWebApi {

	ActionEventStart("/action/eventstart"),

	ActionEventEnd("/action/eventend"),

	ActionSnapshot("/action/snapshot"),

	ActionRestart("/action/restart"),

	ActionQuit("/action/quit"),

	ActionEnd("/action/end"),

	DetectionConnection("/detection/connection"),

	DetectionStart("/detection/start"),

	DetectionStatus("/detection/status"),

	DetectionPause("/detection/pause"),

	TrackCenter("/track/center"),

	TrackSet("/track/set");

	private String path;

	private MotionWebApi(String path) {
		this.path = path;
	}

	/**
	 * Get the web control API method path, relative to the leading camera ID
	 * path segment.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get an absolute URL for this path.
	 * 
	 * @param baseUrl
	 *        the base URL
	 * @param cameraId
	 *        the camera ID
	 * @param queryParameters
	 *        optional query parameters to add to the URL
	 * @return the URL string
	 */
	public String absoluteUrl(String baseUrl, int cameraId, Map<String, Object> queryParameters) {
		StringBuilder buf = new StringBuilder();
		if ( baseUrl != null ) {
			buf.append(baseUrl);
		}
		if ( buf.length() > 0 && buf.charAt(buf.length() - 1) != '/' ) {
			buf.append('/');
		}
		buf.append(cameraId).append(path);
		if ( queryParameters != null && !queryParameters.isEmpty() ) {
			buf.append('?').append(UrlUtils.urlEncoded(queryParameters));
		}
		return buf.toString();
	}

}
