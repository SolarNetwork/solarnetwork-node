/* ==================================================================
 * SolarNodeDialect.java - 17/06/2025 5:45:02 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.thymeleaf;

import java.util.LinkedHashSet;
import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.dialect.IProcessorDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.processor.StandardXmlNsTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * SolarNode dialect for Thymeleaf.
 *
 * @author matt
 * @version 1.0
 */
public class SolarNodeDialect extends AbstractProcessorDialect
		implements IProcessorDialect, IExpressionObjectDialect {

	/** The dialect name. */
	public static final String NAME = "SolarNode";

	/** The dialect prefix. */
	public static final String PREFIX = "snode";

	/** The default processor precedence. */
	public static final int DEFAULT_ROCESSOR_PRECEDENCE = 2000;

	private final SolarNodeExperssionObjectFactory expressionObjectFactory;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_ROCESSOR_PRECEDENCE} will be used.
	 * </p>
	 */
	public SolarNodeDialect() {
		this(DEFAULT_ROCESSOR_PRECEDENCE);
	}

	/**
	 * Constructor.
	 *
	 * @param processorPrecedence
	 *        the processor precedence to use
	 */
	public SolarNodeDialect(int processorPrecedence) {
		super(NAME, PREFIX, processorPrecedence);
		this.expressionObjectFactory = new SolarNodeExperssionObjectFactory();
	}

	@Override
	public Set<IProcessor> getProcessors(String dialectPrefix) {
		var result = new LinkedHashSet<IProcessor>(8);
		result.add(new ResourcesElementTagProcessor(dialectPrefix));
		result.add(new InlineResourcesElementModelProcessor(dialectPrefix));
		result.add(new MessageElementTagProcessor(dialectPrefix));

		// remove the xmlns:snode from output
		result.add(new StandardXmlNsTagProcessor(TemplateMode.HTML, dialectPrefix));
		return result;
	}

	@Override
	public IExpressionObjectFactory getExpressionObjectFactory() {
		return expressionObjectFactory;
	}

}
