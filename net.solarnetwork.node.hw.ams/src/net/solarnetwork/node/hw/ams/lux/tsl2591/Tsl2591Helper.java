/* ==================================================================
 * Tsl2591Helper.java - 1/09/2020 6:58:05 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ams.lux.tsl2591;

import static java.math.BigDecimal.ONE;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dvlopt.linux.i2c.I2CBus;
import io.dvlopt.linux.i2c.SMBus;
import net.solarnetwork.domain.Bitmaskable;

/**
 * Implementation of {@link Tsl2591Operations}.
 * 
 * @author matt
 * @version 1.0
 */
public class Tsl2591Helper implements Tsl2591Operations {

	public static final BigDecimal LUX_DF = new BigDecimal("408.0");
	public static final BigDecimal LUX_COEFB = new BigDecimal("1.64"); // CH0 coefficient
	public static final BigDecimal LUX_COEFC = new BigDecimal("0.59"); // CH1 coefficient A
	public static final BigDecimal LUX_COEFD = new BigDecimal("0.86"); // CH2 coefficient B

	private static final Logger log = LoggerFactory.getLogger(Tsl2591Helper.class);

	private final String devicePath;
	private final int address;
	private Gain gain;
	private IntegrationTime integrationTime;

	private I2CBus i2c;

	/**
	 * Constructor.
	 * 
	 * @param devicePath
	 *        the device path, e.g. <code>/dev/i2c-0</code>
	 * @param address
	 *        the I2C 7-bit address of the sensor
	 */
	public Tsl2591Helper(String devicePath, int address) {
		super();
		this.devicePath = devicePath;
		this.address = address;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Tsl2591Helper{");
		builder.append(address);
		builder.append("@");
		builder.append(devicePath);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public synchronized void close() throws IOException {
		if ( i2c != null ) {
			i2c.close();
			i2c = null;
		}
	}

	private synchronized I2CBus getBus() throws IOException {
		I2CBus b = this.i2c;
		if ( b == null ) {
			b = new I2CBus(this.devicePath);
			b.selectSlave(address);
			this.i2c = b;
		}
		return b;
	}

	private static String hexByte(int val) {
		return String.format("%02X", val & 0xFF);
	}

	private static String hexWord(int val) {
		return String.format("%04X", val & 0xFFFF);
	}

	private void writeByte(SMBus smbus, Register reg, int val) throws IOException {
		smbus.writeByte(reg.commandValue(), val);
		if ( log.isTraceEnabled() ) {
			log.trace("Wrote byte 0x{} to {} (0x{}) on TSL25991 {}@{}", hexByte(val), reg,
					hexByte(reg.commandValue()), address, devicePath);
		}
	}

	private int readWord(SMBus smbus, Register reg) throws IOException {
		int val = smbus.readWord(reg.commandValue());
		if ( log.isTraceEnabled() ) {
			log.trace("Read word 0x{} from {} (0x{}) on TSL25991 {}@{}", hexWord(val), reg,
					hexByte(reg.commandValue()), address, devicePath);
		}
		return val;
	}

	@Override
	public void setup(Gain gain, IntegrationTime integrationTime) throws IOException {
		I2CBus bus = getBus();
		int g = (gain != null ? gain : Gain.Medium).getCode();
		int t = (integrationTime != null ? integrationTime : IntegrationTime.Time100ms).getCode();
		int val = ((g & 0x3) << 4) | (t & 0x7);
		log.info("Setting up gain {}, integration time {} on TSL2591 {}@{}", gain, integrationTime,
				address, devicePath);
		writeByte(bus.smbus, Register.Control, val);
		this.gain = gain;
		this.integrationTime = integrationTime;
	}

	@Override
	public void setEnableModes(Set<EnableMode> modes) throws IOException {
		int val = Bitmaskable.bitmaskValue(modes);
		log.info("Enabling modes {} on TSL25991 {}@{}", modes, address, devicePath);
		I2CBus bus = getBus();
		writeByte(bus.smbus, Register.Enable, val);
	}

	@Override
	public BigDecimal getLux() throws IOException {
		I2CBus bus = getBus();
		int ch0 = readWord(bus.smbus, Register.Channel0);
		int ch1 = readWord(bus.smbus, Register.Channel1);
		long atime = (integrationTime != null ? integrationTime : IntegrationTime.Time100ms)
				.getDuration();
		int again = (gain != null ? gain : Gain.Low).getMultiplier();

		log.debug("Read luminisoty data from {}@{}: {} {}", address, devicePath, ch0, ch1);

		// cpl = (atime * again)
		BigDecimal cpl = new BigDecimal(atime * again).setScale(9).divide(LUX_DF, RoundingMode.HALF_UP);

		// See: https://github.com/adafruit/Adafruit_TSL2591_Library/issues/14
		// lux = (ch0 - ch1) * (1.0F - (ch1 / ch0)) / cpl;
		BigDecimal c0 = new BigDecimal(ch0);
		BigDecimal c1 = new BigDecimal(ch1);

		// @formatter:off
		return c0.subtract(c1).multiply(
					ONE.subtract(c1.setScale(9).divide(c0, RoundingMode.HALF_UP)))
				.divide(cpl, RoundingMode.HALF_UP);
		// @formatter:on
	}

	/**
	 * Get the device path.
	 * 
	 * @return the device path
	 */
	public String getDevicePath() {
		return devicePath;
	}

	/**
	 * Get the gain.
	 * 
	 * @return the gain last set via {@link #setup(Gain, IntegrationTime)}
	 */
	public Gain getGain() {
		return gain;
	}

	/**
	 * Get the integration time.
	 * 
	 * @return the integration time last set via
	 *         {@link #setup(Gain, IntegrationTime)}
	 */
	public IntegrationTime getIntegrationTime() {
		return integrationTime;
	}

}
