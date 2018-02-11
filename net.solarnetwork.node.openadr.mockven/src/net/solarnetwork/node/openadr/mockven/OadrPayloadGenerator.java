
package net.solarnetwork.node.openadr.mockven;

import java.util.Random;

public abstract class OadrPayloadGenerator {

	String genRandomRequestID() {
		Random rand = new Random();
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < 10; i++ ) {
			//this produces lowercase letters
			sb.append(Integer.toHexString(rand.nextInt(16)));
		}
		return sb.toString();
	}
}
