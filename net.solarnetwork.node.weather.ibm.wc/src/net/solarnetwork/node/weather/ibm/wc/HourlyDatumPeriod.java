
package net.solarnetwork.node.weather.ibm.wc;

public enum HourlyDatumPeriod {

	SIXHOUR("6hour"),
	TWELVEHOUR("12hour"),
	ONEDAY("1day"),
	TWODAY("2day"),
	THREEDAY("3day"),
	TENDAY("10day"),
	FIFTEENDAY("15day");

	private final String period;

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

	public static HourlyDatumPeriod forPeriod(String period) {
		for ( HourlyDatumPeriod e : HourlyDatumPeriod.values() ) {
			if ( e.getPeriod().equalsIgnoreCase(period) ) {
				return e;
			}
		}
		return null;// not found
	}

}
