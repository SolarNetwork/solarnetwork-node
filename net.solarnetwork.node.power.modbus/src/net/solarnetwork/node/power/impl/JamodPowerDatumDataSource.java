/* ===================================================================
 * JamodPowerDatumDataSource.java
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

package net.solarnetwork.node.power.impl;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.power.PowerDatum;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

/**
 * {@link GenerationDataSource} implementation using the Jamod modbus
 * serial communication implementation.
 * 
 * <p>This implementation is not very refined, and was written to support one
 * specific Morningstar TS-45 charge controller, but ideally this class could
 * be cleaned up to support any Modbus-based controller. As of now there are
 * many hard-coded constants in this class that probably make this useless for
 * other controllers besides the TS-45.</p>
 * 
 * <p>Pass -Dnet.wimpi.modbus.debug=true to the JVM to enable Jamod debug
 * communication output to STDOUT.</p>
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public class JamodPowerDatumDataSource implements DatumDataSource<PowerDatum> {
	
	private static final SerialParameters DEFAULT_SERIAL_PARAMS = new SerialParameters();
	
	static {
		DEFAULT_SERIAL_PARAMS.setPortName("/dev/ttyS0");
		DEFAULT_SERIAL_PARAMS.setBaudRate(9600);
		DEFAULT_SERIAL_PARAMS.setDatabits(8);
		DEFAULT_SERIAL_PARAMS.setParity("None");
		DEFAULT_SERIAL_PARAMS.setStopbits(2);
		DEFAULT_SERIAL_PARAMS.setEncoding("rtu");
		DEFAULT_SERIAL_PARAMS.setEcho(false);
		DEFAULT_SERIAL_PARAMS.setReceiveTimeout(1600);
	}
	
	private int unitId = 1;
	private int register = 8;
	private int count = 5;
	private int transactionRepeat = 1;
	private SerialParameters serialParameters;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public PowerDatum readCurrentDatum() {
		SerialConnection conn = null;
		try {
			conn = openConnection(2);
			return getSample(conn);
		} finally {
			if ( conn != null ) {
				conn.close();
			}
		}
	}
	
	private PowerDatum getSample(SerialConnection conn) {
		final PowerDatum datum = new PowerDatum();
		
		// first transaction: pv volts, pv amps, and battery volts
		readInputRegister(conn, 
			new ReadInputRegistersRequest(this.register, this.count), 
			this.transactionRepeat, 
			new ReadInputRegisterTransactionCallback() {
				public void doInTransaction(
						ReadInputRegistersResponse res, int transactionIdx) {
					
					if ( log.isDebugEnabled() ) {
						log.debug("res.getWordCount(): " +res.getWordCount() );
					}
					
					FOR: for ( int n = 0; n < res.getWordCount(); n++) {
						int regVal = res.getRegisterValue(n);
						
						if ( log.isDebugEnabled() ) {
							log.debug("regVal: " + n + " = " + regVal );
						}
						
						// FIXME define configurable properties for 'n' values
						switch ( n ) {
						case 0:
							// FIXME define constants for these values
							datum.setBatteryVolts((float)((regVal * 96.667) / 32768));
							
							if ( log.isDebugEnabled() ) {
								log.debug("setBatteryVolts: " +((regVal * 96.667) / 32768) );
							}
							
							break ;
							
						case 2:
							datum.setPvVolts((float)((regVal * 139.15) / 32768));
							
							if ( log.isDebugEnabled() ) {
								log.debug("setPvVolts: " +((regVal * 139.15) / 32768) );
							}
							break ;
							
						case 3:
							datum.setPvAmps((float)((regVal * 66.667) / 32768));
							
							if ( log.isDebugEnabled() ) {
								log.debug("setPvAmps: " +((regVal * 66.667) / 32768));
							}
							break ;
	
						case 4:
							datum.setDcOutputAmps((float)((regVal * 316.667) / 32768));
							break FOR;
							
						}
					}
				}
			});
		
		readInputRegister(conn, 
			// FIXME define configurable properties for these values
			new ReadInputRegistersRequest(16, 5), 
			this.transactionRepeat, 
			new ReadInputRegisterTransactionCallback() {
				public void doInTransaction(
						ReadInputRegistersResponse res, int transactionIdx) {
					
					//grab both high and low amp hour registers
					double theResettableHighAmpHourWord = 0;
					double theResettableLowAmpHourWord = 0;
					
					if ( log.isDebugEnabled() ) {
						log.debug("amphour res.getWordCount(): " + res.getWordCount() );
					}
					
					FOR: for ( int n = 0; n < res.getWordCount(); n++) {
						int regVal = res.getRegisterValue(n);
						
						if ( log.isDebugEnabled() ) {
							log.debug("for amp hours regVal: " + n + " = " + regVal );
						}
						
						// FIXME define configurable properties for 'n' values
						switch ( n ) {
						case 1:
							if ( log.isDebugEnabled() ) {
								log.debug("High amp hour word: " +regVal);
							}
							theResettableHighAmpHourWord = regVal * 0.1;
							break ;
							
						case 2:
							if ( log.isDebugEnabled() ) {
								log.debug("Low amp hour word: " +regVal);
							}
							theResettableLowAmpHourWord = regVal;
							break FOR;
							
						}
					}
				
					if ( log.isDebugEnabled() ) {
						log.debug("theResettableHighAmpHourWord: " +theResettableHighAmpHourWord);
						log.debug("theResettableLowAmpHourWord: " +theResettableLowAmpHourWord);
					}
					
					//add the two values together  FIXME should use the system voltage not 12
					datum.setAmpHoursToday(theResettableHighAmpHourWord + theResettableLowAmpHourWord);
					datum.setKWattHoursToday((theResettableHighAmpHourWord + theResettableLowAmpHourWord) * 12 / 1000);
				
					if ( log.isDebugEnabled() ) {
						log.debug("getAmpHoursToday: " +datum.getAmpHoursToday());
						log.debug("getKWattHoursToday: " +datum.getKWattHoursToday());
					}
				
				}
			});
		
		return datum;
	}
	
	private void readInputRegister(SerialConnection conn, 
			ReadInputRegistersRequest req, 
			int txRepeat,
			ReadInputRegisterTransactionCallback callback) {
		if ( log.isInfoEnabled() ) {
			log.info("Starting modbus transaction for request [" +req +']');
		}
		
		ModbusSerialTransaction trans = new ModbusSerialTransaction(conn);
		req.setUnitID(this.unitId);
		req.setHeadless();
		trans.setRequest(req);
		for ( int i = 0; i < txRepeat; i++ ) {
			try {
				trans.execute();
			} catch (ModbusException e) {
				throw new RuntimeException(e);
			}
			ReadInputRegistersResponse res = (ReadInputRegistersResponse)trans.getResponse();
			if ( log.isDebugEnabled() ) {
				log.debug("Got response [" +res +']');
			}
			callback.doInTransaction(res, i);
		}
	}
	
	private SerialConnection openConnection(int tries) {
		SerialParameters serialParams = null;
		if ( serialParameters != null ) {
			serialParams = serialParameters;
		} else {
			serialParams = getDefaultSerialParametersInstance();
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Opening serial connection to ["
					+serialParams.getPortName() +"], " +tries +" tries remaining");
		}
		try {
			SerialConnection conn = new SerialConnection(serialParams);
			if ( log.isTraceEnabled() ) {
				log.trace("just before opening connection status:["+ conn.isOpen() +"]");
			}
			conn.open();
			if ( log.isTraceEnabled() ) {
				log.trace("just after opening connection status:["+ conn.isOpen() +"]");
			}
			return conn;
		} catch ( Exception e ) {
			if ( tries > 1 ) {
				return openConnection(tries - 1);
			}
			throw new RuntimeException("Unable to open serial connection to ["
						+serialParams.getPortName() +"]", e);
		}
	}

	private static SerialParameters getDefaultSerialParametersInstance() {
		SerialParameters params = new SerialParameters();
		BeanUtils.copyProperties(DEFAULT_SERIAL_PARAMS, params);
		return params;
	}

	private static interface ReadInputRegisterTransactionCallback {
		
		/**
		 * Execute within a ReadInputRegister transaction.
		 * @param res the response
		 * @param transactionIdx the transaction index
		 */
		void doInTransaction(ReadInputRegistersResponse res, int transactionIdx);
		
	}

	/**
	 * @return the unitId
	 */
	public int getUnitId() {
		return unitId;
	}

	/**
	 * @param unitId the unitId to set
	 */
	public void setUnitId(int unitId) {
		this.unitId = unitId;
	}

	/**
	 * @return the register
	 */
	public int getRegister() {
		return register;
	}

	/**
	 * @param register the register to set
	 */
	public void setRegister(int register) {
		this.register = register;
	}

	/**
	 * @return the transactionRepeat
	 */
	public int getTransactionRepeat() {
		return transactionRepeat;
	}

	/**
	 * @param transactionRepeat the transactionRepeat to set
	 */
	public void setTransactionRepeat(int transactionRepeat) {
		this.transactionRepeat = transactionRepeat;
	}
	
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return the serialParameters
	 */
	public SerialParameters getSerialParameters() {
		return serialParameters;
	}

	/**
	 * @param serialParameters the serialParameters to set
	 */
	public void setSerialParameters(SerialParameters serialParameters) {
		this.serialParameters = serialParameters;
	}

}
