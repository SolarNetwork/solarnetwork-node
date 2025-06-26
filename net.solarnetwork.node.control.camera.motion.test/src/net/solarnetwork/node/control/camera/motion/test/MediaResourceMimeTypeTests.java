/* ==================================================================
 * MediaResourceMimeTypeTests.java - 21/10/2019 10:34:35 am
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

package net.solarnetwork.node.control.camera.motion.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Test;
import net.solarnetwork.node.control.camera.motion.MediaResourceMimeType;

/**
 * Test cases for the {@link MediaResourceMimeType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MediaResourceMimeTypeTests {

	@Test
	public void unknownFilename() {
		MediaResourceMimeType result = MediaResourceMimeType.forFilename("foo.whattheheck");
		assertThat("Type is not resolved", result, nullValue());
	}

	@Test
	public void jpgFilename() {
		MediaResourceMimeType result = MediaResourceMimeType.forFilename("foo.jpg");
		assertThat("Type is resolved", result, equalTo(MediaResourceMimeType.JPEG));
	}

	@Test
	public void jpegFilename() {
		MediaResourceMimeType result = MediaResourceMimeType.forFilename("foo.jpeg");
		assertThat("Type is resolved", result, equalTo(MediaResourceMimeType.JPEG));
	}

	@Test
	public void pngFilename() {
		MediaResourceMimeType result = MediaResourceMimeType.forFilename("foo.png");
		assertThat("Type is resolved", result, equalTo(MediaResourceMimeType.PNG));
	}

}
