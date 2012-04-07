/* ==================================================================
 * InstructionStatusPropertyEditor.java - Oct 1, 2011 4:28:06 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.reactor.support;

import java.beans.PropertyEditorSupport;
import java.util.Date;

import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * PropertyEditor for {@link InstructionStatus} objects.
 * 
 * @author matt
 * @version $Revision$
 */
public class InstructionStatusPropertyEditor extends PropertyEditorSupport
implements Cloneable {

	@Override
	public String getAsText() {
		Object val = getValue();
		if ( val == null ) {
			return null;
		}
		if ( val instanceof InstructionStatus  ) {
			return ((InstructionStatus)val).getInstructionState().toString();
		} 
		throw new IllegalArgumentException("Unsupported duration object [" 
				+val.getClass() +"]: " +val);
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		InstructionStatus.InstructionState state = 
				InstructionStatus.InstructionState.valueOf(text);
		setValue(new BasicInstructionStatus(null, state, new Date()));
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

}
