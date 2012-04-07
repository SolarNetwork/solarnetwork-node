/* ==================================================================
 * UnicodeReader.java - Oct 2, 2011 9:55:32 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;

/**
 * Reader that can detect a Unicode BOM.
 * 
 * Adapted from Google Data API.
 * 
 * @author matt
 * @version $Revision$
 */
public class UnicodeReader extends Reader {

    private static final int BOM_SIZE = 4;
    private final InputStreamReader reader;

    /**
     * Construct UnicodeReader.
     * 
     * @param in Input stream.
     * @param defaultEncoding Default encoding to be used if BOM is not found,
     * or <code>null</code> to use system default encoding.
     * @throws IOException If an I/O error occurs.
     */
    public UnicodeReader(InputStream in, String defaultEncoding) throws IOException {
        byte bom[] = new byte[BOM_SIZE];
        String encoding;
        int unread;
        PushbackInputStream pushbackStream = new PushbackInputStream(in, BOM_SIZE);
        int n = pushbackStream.read(bom, 0, bom.length);

        // Read ahead four bytes and check for BOM marks.
        if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            encoding = "UTF-8";
            unread = n - 3;
        } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
            encoding = "UTF-16BE";
            unread = n - 2;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
            encoding = "UTF-16LE";
            unread = n - 2;
        } else if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
            encoding = "UTF-32BE";
            unread = n - 4;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
            encoding = "UTF-32LE";
            unread = n - 4;
        } else {
            encoding = defaultEncoding;
            unread = n;
        }

        // Unread bytes if necessary and skip BOM marks.
        if (unread > 0) {
            pushbackStream.unread(bom, (n - unread), unread);
        } else if (unread < -1) {
            pushbackStream.unread(bom, 0, 0);
        }

        // Use given encoding.
        if (encoding == null) {
            reader = new InputStreamReader(pushbackStream);
        } else {
            reader = new InputStreamReader(pushbackStream, encoding);
        }
    }

    public String getEncoding() {
        return reader.getEncoding();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    public void close() throws IOException {
        reader.close();
    }

}
