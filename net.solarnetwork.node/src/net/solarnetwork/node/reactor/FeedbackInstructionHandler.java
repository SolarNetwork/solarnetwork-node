/* ==================================================================
 * FeedbackInstructionHandler.java - 19/06/2017 1:07:53 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.reactor;

/**
 * Extension of {@link InstructionHandler} that provides feedback on the
 * instruction status.
 * 
 * @author matt
 * @version 1.0
 * @since 1.50
 */
public interface FeedbackInstructionHandler extends InstructionHandler {

	/**
	 * Process an instruction, providing status feedback.
	 * 
	 * @param instruction
	 *        the instruction to process
	 * @return new status the instruction, or <em>null</em> if the instruction
	 *         was not handled
	 */
	InstructionStatus processInstructionWithFeedback(Instruction instruction);

}
