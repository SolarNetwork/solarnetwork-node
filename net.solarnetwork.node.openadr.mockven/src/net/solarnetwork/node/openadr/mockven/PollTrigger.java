
package net.solarnetwork.node.openadr.mockven;

import java.util.Date;
import org.quartz.impl.triggers.SimpleTriggerImpl;

/**
 * 
 * FIXME
 * 
 * This class currently does not do what I want it to do. I want to be able to
 * poll the VTN every 10 seconds. Currently instead I am using a CronTrigger
 * which limits my polling to every minute or second.
 * 
 * @author robert
 * @version 1.0
 */
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
