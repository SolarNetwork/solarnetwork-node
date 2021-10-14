/* ==================================================================
 * JobSettingSpecifierProvider.java - Mar 20, 2012 9:10:44 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.quartz.CronTrigger;
import org.quartz.Trigger;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.util.StringUtils;
import net.solarnetwork.node.job.TriggerAndJobDetail;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.TextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link SettingSpecifierProvider} for Quartz job triggers.
 * 
 * <p>
 * This class allows a set of related Quartz Trigger instances to be exposed as
 * a {@link SettingSpecifierProvider} and managed by the node's setting UI. For
 * example the triggers might be grouped by the bundle they were published from.
 * It works with {@link TriggerAndJobDetail} services. Call
 * {@link #addSpecifier(TriggerAndJobDetail)} and
 * {@link #removeSpecifier(TriggerAndJobDetail)} as {@link TriggerAndJobDetail}
 * services are registered/unregistered with the system.
 * </p>
 * 
 * <p>
 * Currently only {@link CronTrigger} triggers are supported. Localized messages
 * are not supported.
 * </p>
 * 
 * @author matt
 * @version 3.0
 * @deprecated
 */
@Deprecated
public class JobSettingSpecifierProvider implements SettingSpecifierProvider {

	/** The suffix added to display titles. */
	public static final String TITLE_SUFFIX = " Jobs";

	/** The suffix to remove from display titles in setting UIDs. */
	public static final String JOBS_PID_SUFFIX = ".JOBS";

	/** The prefix to remove for display titles in setting UIDs. */
	public static final String SN_NODE_PREFIX = "net.solarnetwork.node.";

	private String settingUid;
	private MessageSource messageSource = null;
	private List<SettingSpecifier> specifiers = new ArrayList<SettingSpecifier>();
	private final Map<String, MessageFormat> messages = new HashMap<String, MessageFormat>();

	/**
	 * Construct with settings UID.
	 * 
	 * @param settingUid
	 *        the setting UID
	 */
	public JobSettingSpecifierProvider(String settingUid) {
		this(settingUid, null);
	}

	/**
	 * Construct with settings UID.
	 * 
	 * @param settingUid
	 *        the setting UID
	 * @param source
	 *        a message source
	 */
	public JobSettingSpecifierProvider(String settingUid, MessageSource source) {
		super();
		this.settingUid = settingUid;
		this.messageSource = source;
		if ( source == null || !hasMessage(source, "title") ) {
			messages.put("title", new MessageFormat(titleValue(settingUid)));
		}

		AbstractMessageSource msgSource = new AbstractMessageSource() {

			@Override
			protected MessageFormat resolveCode(String code, Locale locale) {
				return messages.get(code);
			}
		};
		msgSource.setParentMessageSource(source);
		this.messageSource = msgSource;
	}

	private static boolean hasMessage(MessageSource source, String key) {
		if ( source == null ) {
			return false;
		}
		try {
			source.getMessage(key, null, Locale.getDefault());
			return true;
		} catch ( NoSuchMessageException e ) {
			return false;
		}
	}

	/**
	 * Construct a display title based on a setting UID.
	 * 
	 * <p>
	 * The display title is generated from the setting UID itself, by first
	 * removing the {@link #SN_NODE_PREFIX} prefix and {@link #JOBS_PID_SUFFIX}
	 * suffix, capitalizing the remaining value, and appending
	 * {@link #TITLE_SUFFIX}. For example, the UID
	 * <code>net.solarnetwork.node.power.JOBS</code> will result in
	 * <code>Power Jobs</code>.
	 * </p>
	 * 
	 * 
	 * @param settingUid
	 *        the setting UID value
	 * @return the generated title value
	 */
	private static String titleValue(String settingUid) {
		if ( settingUid.startsWith(SN_NODE_PREFIX) && settingUid.length() > SN_NODE_PREFIX.length() ) {
			String subPackage = settingUid.substring(SN_NODE_PREFIX.length());
			if ( subPackage.endsWith(JOBS_PID_SUFFIX)
					&& subPackage.length() > JOBS_PID_SUFFIX.length() ) {
				subPackage = subPackage.substring(0, subPackage.length() - JOBS_PID_SUFFIX.length());
			}
			if ( subPackage.indexOf('.') < 0 ) {
				// capitalize first letter
				subPackage = Character.toUpperCase(subPackage.charAt(0)) + subPackage.substring(1)
						+ TITLE_SUFFIX;
				return subPackage;
			}
			return subPackage;
		}
		return settingUid;
	}

	/**
	 * Create appropriate {@link SettingSpecifier} instances for a given
	 * {@link TriggerAndJobDetail}.
	 * 
	 * <p>
	 * Call this method for every {@link TriggerAndJobDetail} published in the
	 * system.
	 * </p>
	 * 
	 * @param trigJob
	 *        the service to generate specifiers for
	 */
	public void addSpecifier(TriggerAndJobDetail trigJob) {
		Trigger trig = trigJob.getTrigger();
		if ( trig instanceof CronTrigger ) {
			CronTrigger ct = (CronTrigger) trig;
			final String key = JobUtils.triggerKey(ct);
			BasicTextFieldSettingSpecifier tf = new BasicTextFieldSettingSpecifier(key,
					ct.getCronExpression());
			tf.setTitle(ct.getKey().getName());
			final String labelKey = key + ".key";
			final String descKey = key + ".desc";
			if ( !hasMessage(this.messageSource, labelKey) ) {
				if ( hasMessage(trigJob.getMessageSource(), labelKey) ) {
					messages.put(labelKey, new MessageFormat(
							trigJob.getMessageSource().getMessage(labelKey, null, Locale.getDefault())));
				} else {
					messages.put(labelKey, new MessageFormat(ct.getKey().getName()));
				}
			}
			if ( !hasMessage(this.messageSource, descKey) ) {
				if ( hasMessage(trigJob.getMessageSource(), labelKey) ) {
					messages.put(descKey, new MessageFormat(
							trigJob.getMessageSource().getMessage(descKey, null, Locale.getDefault())));
				} else {
					messages.put(descKey, new MessageFormat(
							StringUtils.hasText(ct.getDescription()) ? ct.getDescription() : ""));
				}
			}
			synchronized ( specifiers ) {
				specifiers.add(tf);
			}
		}
	}

	/**
	 * Remove {@link SettingSpecifier} instances previously registered via
	 * {@link #addSpecifier(TriggerAndJobDetail)} for a given
	 * {@link TriggerAndJobDetail}.
	 * 
	 * <p>
	 * Call this method for every {@link TriggerAndJobDetail} unregistered in
	 * the system.
	 * </p>
	 * 
	 * @param trigJob
	 *        the service to generate specifiers for
	 */
	public void removeSpecifier(TriggerAndJobDetail trigJob) {
		Trigger trig = trigJob.getTrigger();
		if ( trig instanceof CronTrigger ) {
			CronTrigger ct = (CronTrigger) trig;
			final String key = JobUtils.triggerKey(ct);
			synchronized ( specifiers ) {
				for ( Iterator<SettingSpecifier> itr = specifiers.iterator(); itr.hasNext(); ) {
					TextFieldSettingSpecifier tf = (TextFieldSettingSpecifier) itr.next();
					if ( tf.getKey().equals(key) ) {
						itr.remove();
						break;
					}
				}
			}
		}
	}

	@Override
	public String getSettingUid() {
		return settingUid;
	}

	@Override
	public String getDisplayName() {
		return settingUid;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		synchronized ( specifiers ) {
			return Collections.unmodifiableList(specifiers);
		}
	}

	public List<SettingSpecifier> getSpecifiers() {
		return specifiers;
	}

	public void setSpecifiers(List<SettingSpecifier> specifiers) {
		this.specifiers = specifiers;
	}

}
