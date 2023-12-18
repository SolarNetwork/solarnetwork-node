/* ==================================================================
 * ProcessActionCommandRunner.java - 13/08/2018 10:29:20 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.os.stat;

import static net.solarnetwork.node.Constants.solarNodeHome;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.util.StringUtils;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Implementation of {@link ActionCommandRunner} that executes an external OS
 * command.
 * 
 * @author matt
 * @version 2.1
 */
public class ProcessActionCommandRunner implements ActionCommandRunner, SettingSpecifierProvider {

	/** The default value for the {@code command} property. */
	public static final String DEFAULT_COMMAND = solarNodeHome() + "/bin/solarstat";

	private String command = DEFAULT_COMMAND;
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public ProcessActionCommandRunner() {
		super();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.os.stat.ProcessActionCommandRunner";
	}

	@Override
	public String getDisplayName() {
		return "OS Action Command Runner";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.singletonList(new BasicTextFieldSettingSpecifier("command", DEFAULT_COMMAND));
	}

	/**
	 * Parse action command output in CSV form.
	 * 
	 * @param input
	 *        the input stream to parse
	 * @return the parsed rows
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static List<Map<String, String>> parseActionCommandCsvOutput(InputStream input)
			throws IOException {
		List<Map<String, String>> result = new ArrayList<>();
		BufferedReader in = new BufferedReader(new InputStreamReader(input));
		String line = null;
		String[] columns = null;
		while ( (line = in.readLine()) != null ) {
			if ( columns == null ) {
				columns = StringUtils.commaDelimitedListToStringArray(line);
			} else {
				Map<String, String> data = new LinkedHashMap<>(columns.length);
				String[] row = StringUtils.commaDelimitedListToStringArray(line);
				for ( int i = 0; i < row.length && i < columns.length; i++ ) {
					data.put(columns[i], row[i]);
				}
				result.add(data);
			}
		}
		return result;
	}

	@Override
	public List<Map<String, String>> executeAction(final String action) {
		log.debug("Executing action {}", action);
		ProcessBuilder pb = new ProcessBuilder(new String[] { command, action });
		try {
			Process pr = pb.start();

			List<Map<String, String>> result = parseActionCommandCsvOutput(pr.getInputStream());

			BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			StringBuilder buf = new StringBuilder();
			String line = null;
			while ( (line = err.readLine()) != null ) {
				if ( buf.length() > 0 ) {
					buf.append('\n');
				}
				buf.append(line);
			}
			if ( buf.length() > 0 ) {
				log.error("Error executing action {}: {}", action, buf);
			}
			return result;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set the helper program command to use.
	 * 
	 * <p>
	 * This defaults to {@link #DEFAULT_COMMAND}.
	 * </p>
	 * 
	 * @param command
	 *        the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * The message source to use for settings.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
