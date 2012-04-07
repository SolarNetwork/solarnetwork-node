/* ===================================================================
 * Handler.java
 * 
 * Created Oct 12, 2009 8:54:07 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.io.url.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} implementation to read from a socket.
 * 
 * <p>This handler enables a {@code socket://} style URL read-only connection.
 * After constructing the URL, call {@link URLConnection#getInputStream()} to
 * read from the socket.</p>
 * 
 * <p>To enable this handler, pass the following property to the JVM:</p>
 * 
 * <code>-Djava.protocol.handler.pkgs=net.solarnetwork.node.io.url</code>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class Handler extends URLStreamHandler {

	/* (non-Javadoc)
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return new URLConnection(u) {
			Socket socket = null;

			@Override
			public void connect() throws IOException {
				socket = new Socket(getURL().getHost(), getURL().getPort());
				if ( getConnectTimeout() > 0 ) {
					socket.setSoTimeout(getConnectTimeout());
				}
			}

			@Override
			public InputStream getInputStream() throws IOException {
				if ( socket == null ) {
					connect();
				}
				return socket.getInputStream();
			}
			
		};
	}

}
