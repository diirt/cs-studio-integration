/* 
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.sds.internal.model.logic;

import org.csstudio.sds.internal.connection.ChannelReference;
import org.csstudio.sds.internal.statistics.MeasureCategoriesEnum;
import org.csstudio.sds.internal.statistics.TimeTrackedRunnable;
import org.csstudio.sds.model.logic.IRule;

/**
 * A rule engine processes a single rule.
 * 
 * A rule can depend on multiple input channels. Because these channels deliver
 * dynamic data independent from each other, the latest values that were
 * delivered have to be cached in a state object. This caching is the main task
 * of a rule engine.
 * 
 * @author Alexander Will & Sven Wende
 * @version $Revision$
 * 
 */
public final class RuleEngine {
	/**
	 * The associated rule.
	 */
	private IRule _rule;

	/**
	 * The state of this rule engine.
	 */
	private RuleState _state;

	/**
	 * Standard constructor.
	 * 
	 * @param rule
	 *            The rule that is used for the evaluation of the received
	 *            control system events.
	 * @param channelReferences
	 *            references to all input channels, the rule relies on
	 */
	public RuleEngine(final IRule rule,
			final ChannelReference[] channelReferences) {
		assert rule != null;
		assert channelReferences != null;

		_rule = rule;
		_state = new RuleState(channelReferences);
	}

	/**
	 * Updates the latest value for the provided channel and processes the rule
	 * based on the latest input values for all input channels, which have been
	 * cached in a state object.
	 * 
	 * @param channelReference
	 *            a channel reference for the channel which delivered a new
	 *            value
	 * @param newValue
	 *            the new value
	 * 
	 * @return the result of the rule
	 */
	public synchronized Object processRule(
			final ChannelReference channelReference, final Object newValue) {
		// cache the new value
		_state.cacheParameterValue(channelReference, newValue);

		// get all cached values
		Object[] values = _state.getRecentParameterValues();

		// apply the rule
		Object value = _rule.evaluate(values);

		return value;
	}

	/**
	 * Simple wrapper class (needed for the performance tracking).
	 * 
	 * @author Sven Wende
	 * @version $Revision$
	 */
	final class Wrapper {
		/**
		 * Value.
		 */
		private Object _value;

		/**
		 * @return Gets the value.
		 */
		public Object getValue() {
			return _value;
		}

		/**
		 * Sets the value.
		 * 
		 * @param value
		 *            the value
		 */
		public void setValue(final Object value) {
			_value = value;
		}
	}
}
