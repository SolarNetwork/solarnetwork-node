
package net.solarnetwork.node.openadr.mockven;

/**
 * 
 * enum of the OpenADR signal names. perhaps this should be moved into the
 * OpenADR package. Followed the design pattern of SignalTypeEnumeratedType
 * 
 * @author robert
 * @version 1.0
 */
public enum SignalNameEnumeratedType {
	SIMPLE("SIMPLE"),
	simple("simple"),
	ELECTRICITY_PRICE("ELECTRICITY_PRICE"),
	ENERGY_PRICE("ENERGY_PRICE"),
	DEMAND_CHARGE("DEMAND_CHARGE"),
	BID_PRICE("BID_PRICE"),
	BID_LOAD("BID_LOAD"),
	BID_ENERGY("BID_ENERGY"),
	CHARGE_STATE("CHARGE_STATE"),
	LOAD_DISPATCH("LOAD_DISPATCH"),
	LOAD_CONTROL("LOAD_CONTROL");

	private final String value;

	SignalNameEnumeratedType(String v) {
		value = v;
	}

	@Override
	public String toString() {
		return value;
	}

	public static SignalNameEnumeratedType fromValue(String v) {
		for ( SignalNameEnumeratedType c : SignalNameEnumeratedType.values() ) {
			if ( c.value.equals(v) ) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}
