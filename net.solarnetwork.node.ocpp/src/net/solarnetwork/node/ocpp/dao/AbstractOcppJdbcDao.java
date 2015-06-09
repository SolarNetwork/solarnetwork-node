/* ==================================================================
 * AbstractOcppJdbcDao.java - 9/06/2015 1:03:00 pm
 * 
 * Copyright 2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.dao;

import java.util.Calendar;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.solarnetwork.node.dao.jdbc.AbstractJdbcDao;

/**
 * Abstract base class for OCPP related DAOs.
 * 
 * @author matt
 * @version 1.0
 * @param <T>
 *        the primary domain object type managed by this DAO
 */
public abstract class AbstractOcppJdbcDao<T> extends AbstractJdbcDao<T> {

	protected final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	protected final DatatypeFactory datatypeFactory = getDatatypeFactory();
	protected final XMLGregorianCalendar timestampDefaults = getTimestampDefaults();

	protected final Calendar calendarForXMLDate(XMLGregorianCalendar xmlDate) {
		Calendar cal = xmlDate.toGregorianCalendar(null, null, timestampDefaults);
		Calendar utcCal = (Calendar) utcCalendar.clone();
		utcCal.setTimeInMillis(cal.getTimeInMillis());
		return utcCal;
	}

	protected final DatatypeFactory getDatatypeFactory() {
		try {
			return DatatypeFactory.newInstance();
		} catch ( DatatypeConfigurationException e ) {
			throw new RuntimeException(e);
		}
	}

	protected final XMLGregorianCalendar getTimestampDefaults() {
		try {
			DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
			return datatypeFactory.newXMLGregorianCalendarTime(0, 0, 0, 0);
		} catch ( Exception e ) {
			log.error("Exception greating default XMLGregorianCalendar instance", e);
			return null;
		}
	}

}
