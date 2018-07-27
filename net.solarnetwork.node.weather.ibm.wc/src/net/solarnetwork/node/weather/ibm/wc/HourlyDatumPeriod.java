
package net.solarnetwork.node.weather.ibm.wc;

public enum HourlyDatumPeriod implements DatumPeriod {

	SIXHOUR("6hour"),
	TWELVEHOUR("12hour"),
	ONEDAY("1day"),
	TWODAY("2day"),
	THREEDAY("3day"),
	TENDAY("10day"),
	FIFTEENDAY("15day");

	private String period;

	HourlyDatumPeriod(String period) {
		this.period = period;
	}

	public String getPeriod() {
		return this.period;
	}

	@Override
	public String toString() {
		return this.getPeriod();
	}

	static HourlyDatumPeriod getValue(String period) {
		for ( HourlyDatumPeriod e : HourlyDatumPeriod.values() ) {
			if ( e.getPeriod() == period ) {
				return e;
			}
		}
		return null;// not found
	}

}
