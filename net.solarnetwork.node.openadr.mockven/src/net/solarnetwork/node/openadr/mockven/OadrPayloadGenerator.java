
package net.solarnetwork.node.openadr.mockven;

import java.util.Random;

/**
 * Abstract class containing useful functions often used in OadrPayload
 * generators
 * 
 * @author robert
 * @version 1.0
 */
public abstract class OadrPayloadGenerator {

	Random rand = new Random();

	//returns a hex string 10 characters in length used for generating request IDs
	public String genRandomRequestID() {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < 10; i++ ) {
			//this produces lowercase letters
			sb.append(Integer.toHexString(rand.nextInt(16)));
		}
		return sb.toString();
	}

	/**
	 * Dependency Injection for increased controllability to allow testing.
	 * 
	 * @param newrandom
	 */
	public void setRandom(Random newrandom) {
		rand = newrandom;
	}
}
