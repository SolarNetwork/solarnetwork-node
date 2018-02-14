
package net.solarnetwork.node.openadr.mockven;

import java.util.Date;
import org.quartz.impl.triggers.SimpleTriggerImpl;

public class PollTrigger extends SimpleTriggerImpl {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5752379851134828607L;

	public PollTrigger() {
		setStartTime(new Date());
		setEndTime(null);
		setRepeatInterval(10000L);
		setRepeatCount(REPEAT_INDEFINITELY);

	}
}
