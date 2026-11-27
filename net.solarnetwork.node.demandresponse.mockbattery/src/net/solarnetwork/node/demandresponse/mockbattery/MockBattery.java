package net.solarnetwork.node.demandresponse.mockbattery;

public class MockBattery {
	private Double maxcapacity = null;
	private double charge;
	private double draw;
	private long lastsample;

	public MockBattery(double maxcapacity) {
		if (maxcapacity < 0) {
			throw new IllegalArgumentException();
		}
		this.maxcapacity = maxcapacity;
		setCharge(0);
		setDraw(0);
	}

	public MockBattery() {
		this(10.0);
	}

	/**
	 * Sets the maximal charge is kWh of the battery. If the battery's charge is
	 * greater than the new maxcharge there will be no error. However be sure to
	 * call the readCharge method to update the charge value to be within
	 * bounds.
	 * 
	 * @param maxcapacity
	 */
	public void setMax(double maxcapacity) {
		// you cannot have no or negative capacity keep current value if
		// argument is invalid
		if (maxcapacity > 0) {
			this.maxcapacity = maxcapacity;
		}
	}

	/**
	 * Forces the battery to a specific charge at that instant. Try to set a
	 * negative charge will put the battery at 0. Trying to set the charge
	 * beyond max charge will have the battery set to max charge
	 * 
	 * @param charge
	 */
	public void setCharge(double charge) {
		this.lastsample = readTime();
		// can't have negative charge,if that happens we keep the current value
		if (charge >= 0.0) {
			this.charge = Math.min(charge, this.maxcapacity);

		}

	}

	// returns the time difference in hours between now and lastsample reading.
	// This is used for calculating the battery's charge.
	private double deltaTimeHours() {
		long oldtime = this.lastsample;
		long currenttime = readTime();
		this.lastsample = currenttime;
		double delta = currenttime - oldtime;
		delta = delta / 1000 / 60 / 60;
		return delta;
	}

	/**
	 * Returns how many kWh of charge the battery has
	 * 
	 * @return Charge (kWh)
	 */
	public double readCharge() {
		if (this.maxcapacity == null) {
			throw new RuntimeException();
		}
		double delta = deltaTimeHours();
		double newcharge = this.charge - this.draw * delta;
		if (newcharge < 0.0) {
			newcharge = 0;
		}
		if (newcharge > this.maxcapacity) {
			newcharge = this.maxcapacity;
		}
		setCharge(newcharge);
		return newcharge;
	}

	/**
	 * 
	 * @return the powerDraw of the battery (kWh)
	 */
	public double readDraw() {
		if (this.maxcapacity == null) {
			throw new RuntimeException();
		}
		if (this.draw < 0 && this.charge == this.maxcapacity) {
			// can't charge what is already fully charged
			return 0;
		} else if (this.draw > 0 && this.charge == 0) {
			// can't draw from empty battery
			return 0;
		} else {
			return this.draw;
		}
	}

	/**
	 * returns the fraction of remaining battery capacity as a value between 0
	 * and 1. Multiply this value by 100 to get remaining capacity as a
	 * percentage.
	 * 
	 * @return remaining battery life
	 */
	public float capacityFraction() {
		if (this.maxcapacity == null) {
			throw new RuntimeException();
		}
		return (float) (readCharge() / this.maxcapacity);
	}

	/**
	 * Sets the draw of the battery. The mock battery does not have a max or min
	 * draw. A negative draw value means you are charging battery and positive
	 * value means you are draining the battery
	 * 
	 * @param draw
	 */
	public void setDraw(double draw) {
		readCharge();
		this.draw = draw;
	}

	public long readTime() {
		return System.currentTimeMillis();
	}
}
