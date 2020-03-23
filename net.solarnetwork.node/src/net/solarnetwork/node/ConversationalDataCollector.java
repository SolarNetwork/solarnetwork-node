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
 */

package net.solarnetwork.node;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Extension of {@link DataCollector} for two-way conversation based data
 * collecting.
 *
 * @author matt
 * @version 1.0
 */
public interface ConversationalDataCollector extends DataCollector {

	/**
	 * The conversation moderator.
	 * 
	 * @param <T>
	 *        the datum type
	 */
	interface Moderator<T> {

		/**
		 * Start the conversation.
		 * 
		 * @param dataCollector
		 *        the ConversationalDataCollector
		 * @return the datum
		 */
		T conductConversation(ConversationalDataCollector dataCollector);

	}

	/**
	 * API for listening for data with control over how must data is collected.
	 */
	interface DataListener {

		/**
		 * Reset the listener.
		 * 
		 * <p>
		 * This method should be called when re-using an instance to handle
		 * multiple messages.
		 * </p>
		 */
		void reset();

		/**
		 * Get the number of bytes wanted from the serial port.
		 * 
		 * @param dataCollector
		 *        the data collector invoked from
		 * @param sinkSize
		 *        the number of bytes that have been written to the sink
		 * @return the number of bytes wanted
		 */
		int getDesiredByteCount(ConversationalDataCollector dataCollector, int sinkSize);

		/**
		 * Callback when data has been received.
		 * 
		 * <p>
		 * This method is called when some data has been read from the serial
		 * stream. Return {@literal true} to keep listening for more data, or
		 * {@literal false} to stop listening. The received data is not saved
		 * automatically. Copy any desired data to the provided {@code sink}.
		 * </p>
		 * 
		 * @param dataCollector
		 *        the data collector invoked from
		 * @param data
		 *        the data buffer
		 * @param offset
		 *        the offset within the data buffer the data is available at
		 * @param length
		 *        the length of data available in the buffer
		 * @param sink
		 *        an output stream to save received data into
		 * @param sinkSize
		 *        the number of bytes that have been written to the sink
		 * @return {@literal true} to continue listening for data,
		 *         {@literal false} to stop
		 * @throws IOException
		 *         if any IO error occurs
		 */
		boolean receivedData(ConversationalDataCollector dataCollector, byte[] data, int offset,
				int length, OutputStream sink, int sinkSize) throws IOException;

	}

	/**
	 * Collect data with the given Moderator.
	 * 
	 * @param <T>
	 *        the data type
	 * @param moderator
	 *        the conversation moderator
	 * @return the datum
	 */
	<T> T collectData(Moderator<T> moderator);

	/**
	 * Speak without waiting for any response.
	 * 
	 * @param data
	 *        the data to speak
	 */
	void speak(byte[] data);

	/**
	 * Listen for a response, without first speaking.
	 */
	void listen();

	/**
	 * Set a {@link DataListener} to control the listening behavior.
	 * 
	 * @param listener
	 *        the listener
	 */
	void setListener(DataListener listener);

	/**
	 * Remove the {@link DataListener}.
	 * 
	 * 
	 * <p>
	 * Call this method to remove any {@link DataListener} previously set via
	 * {@link #setListener(DataListener)}, {@link #listen(DataListener)}, or
	 * {@link #speakAndListen(byte[], DataListener)}.
	 * </p>
	 */
	void removeListener();

	/**
	 * Listen for a response, without first speaking, using a
	 * {@link DataListener}.
	 * 
	 * <p>
	 * This replaces any previously configured listener.
	 * </p>
	 * 
	 * @param listener
	 *        the listener
	 */
	void listen(DataListener listener);

	/**
	 * Speak and then listen for a response.
	 * 
	 * <p>
	 * Calling code can access the response by calling
	 * {@link #getCollectedData()}.
	 * </p>
	 * 
	 * @param data
	 *        the data to speak
	 */
	void speakAndListen(byte[] data);

	/**
	 * Speak and then listen for a response, using a {@link DataListener}.
	 * 
	 * <p>
	 * This replaces any previously configured listener.
	 * </p>
	 * 
	 * @param data
	 *        the data to speak
	 * @param listener
	 *        the listener
	 */
	void speakAndListen(byte[] data, DataListener listener);

	/**
	 * Speak and then collect data from a response.
	 * 
	 * <p>
	 * The {@code data} will be written to the output stream and then this
	 * method will block until the {@code magic} bytes are read, followed by
	 * {@code length} more bytes. Each invocation of this method will first
	 * clear the internal data buffer, and all received response data will be
	 * stored on the internal data buffer. Calling code can access this buffer
	 * by calling {@link #getCollectedData()}.
	 * </p>
	 * 
	 * <p>
	 * Note the configured {@link DataListener} is not used during this
	 * invocation.
	 * </p>
	 * 
	 * @param data
	 *        the data to write to the serial port
	 * @param magic
	 *        the magic bytes to look for in the response
	 * @param length
	 *        the number of bytes to read, excluding the magic
	 */
	void speakAndCollect(byte[] data, byte[] magic, int length);

}
