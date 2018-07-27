
package net.solarnetwork.node.weather.ibm.wc;

public enum DailyDatumPeriod implements DatumPeriod {
	THREEDAY("3day"),
	FIVEDAY("5day"),
	SEVENDAY("7day"),
	TENDAY("10day"),
	FIFTEENDAY("15day");

	private String period;

	DailyDatumPeriod(String period) {
		this.period = period;
	}

	public String getPeriod() {
		return this.period;
	}

	@Override
	public String toString() {
		return this.getPeriod();
	}

	static DailyDatumPeriod getValue(String period) {
		for ( DailyDatumPeriod e : DailyDatumPeriod.values() ) {
			if ( e.getPeriod() == period ) {
				return e;
			}
		}
		return null;// not found
	}

}
