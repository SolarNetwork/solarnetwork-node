/* ===================================================================
 * ConversationalDataCollector.java
 * 
 * Created Aug 19, 2009 1:26:28 PM
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

package net.solarnetwork.node;

/**
 * Extension of {@link DataCollector} for two-way conversation based
 * data collecting.
 *
 * @author matt
 * @version $Revision$ $Date$
 * @param <T> the datum type
 */
public interface ConversationalDataCollector extends DataCollector {

	/**
	 * The conversation moderator.
	 * @param <T> the datum type
	 */
	public interface Moderator<T> {
		
		/**
		 * Start the conversation.
		 * 
		 * @param dataCollector the ConversationalDataCollector
		 * @return the datum
		 */
		public T conductConversation(ConversationalDataCollector dataCollector);
		
	}
	
	/**
	 * Collect data with the given Moderator.
	 * 
	 * @param moderator the conversation moderator
	 * @return the datum
	 */
	public <T> T collectData(Moderator<T> moderator);
	
	/**
	 * Speak without waiting for any response.
	 * 
	 * @param data the data to speak
	 */
	public void speak(byte[] data);
	
	/**
	 * Speak and then listen for a response.
	 * 
	 * <p>Calling code can access the response by calling 
	 * {@link #getCollectedData()}.</p>
	 * 
	 * @param data the data to speak
	 */
	public void speakAndListen(byte[] data);
	
	/**
	 * Speak and then collect data from a response.
	 * 
	 * <p>The {@code data} will be written to the output stream
	 * and then this method will block until the
	 * {@code magic} bytes are read, followed by {@code length}
	 * more bytes. Calling code can access this buffer by calling 
	 * {@link #getCollectedData()}.</p>
	 * 
	 * @param data the data to write to the serial port
	 * @param magic the magic bytes to look for in the response
	 * @param length the number of bytes to read, excluding the magic
	 */
	public void speakAndCollect(byte[] data, byte[] magic, int length);
	
}
