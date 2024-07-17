$(document).ready(function metricsManagement() {
	'use strict';

	if ( !$('#metrics').length ) {
		return;
	}
	
	/**
	 * Metric entity.
	 * 
	 * @typedef {Object} Metric
	 * @property {string} timestamp the timestamp
	 * @property {string} type the type
	 * @property {string} name the name
	 * @property {string} value the value
	 */
	
	/**
	 * Metric filter results.
	 * 
	 * @typedef {Object} MetricFilterResults
	 * @property {number} returnedResultCount the number of results returned
	 * @property {number} startingOffset the starting offset
	 * @property {Metric[]} results the metrics
	 */

	const metricTemplate = $('#metrics .template');
	const metricContainer = $('#metrics .list-container');
	
	const nameInput = $('#metrics-filter-name');

	const metricRows = new Map();
	
	let pageSize = 25;
	let totalMetricCount = 0;
	let pageOffset = 0;
	
	function setupMetrics(/** @type {MetricFilterResults} */ metrics) {
		metricRows.clear();
		metricContainer.empty();
		
		if ( Array.isArray(metrics.results) ) {
			let row = metrics.startingOffset;
			for ( let metric of metrics.results ) {
				renderMetric(metric, ++row);
			}
		}
			
		$('#metrics .none').toggleClass('hidden', metricRows.size > 0);
		$('#metrics .some').toggleClass('hidden', metricRows.size < 1);
	}

	function toggleLoading(on) {
		$('.init').toggleClass('hidden', !on);
		$('.ready').toggleClass('hidden', on);
	}
	
	function renderMetric(/** @type {Metric} */ metric, /** @type {number} */ row) {
		const itemEl = metricTemplate.clone(true).removeClass('template');
		itemEl.find('[data-tprop=idx]').text(row);
		itemEl.find('[data-tprop=displayTs]').text(moment(metric.timestamp).format('YYYY-MM-DD HH:mm:ss.SSS'));
		itemEl.find('[data-tprop=type]').text(metric.type);
		itemEl.find('[data-tprop=name]').text(metric.name);
		itemEl.find('[data-tprop=value]').text(metric.value);
		
		metricContainer.append(itemEl);
		metricRows.set(metricKey(metric), itemEl);
	}
	
	function metricKey(metric) {
		if (!metric ) {
			return undefined;
		}
		return `${metric.timestamp}.${metric.type}.${metric.name}`;
	}
	
	function handleMetricStoredMessage(msg) {
		/** @type Metric */
		const metric = JSON.parse(msg.body).data;
		if ( !metric ) {
			return;
		}
		console.debug('Metric stored: %o', metric);

		const key = metricKey(metric);
		const itemEl = metricRows.get(key);
		// TODO: handle metric
	}
	
	function queryForMetrics() {
		let url = SolarNode.context.path('/a/metrics/list') 
			+ '?type=s'
			+ '&max=' + pageSize 
			+ '&offset=' +(pageOffset * pageSize)
			;
		if ( nameInput.length > 0 ) {
			const name = nameInput.val();
			if ( name ) {
				url += '&name=' + encodeURIComponent(name);
			}
		}
		// TODO: configurable sorting
		url += '&' + encodeURIComponent('sorts[0].sortKey') + '=date';
		url += '&' + encodeURIComponent('sorts[0].descending') + '=true';
		url += '&' + encodeURIComponent('sorts[1].sortKey') + '=name';
		return $.getJSON(url, (data) => {
			if ( data && data.success === true ) {
				setupMetrics(data.data);
				// TODO subscribe net/solarnetwork/dao/Metric/STORED
			}
		});
	}

	/**
	 * Subscribe to the "datum stored" topic.
	 *
	 * @param {string} [name] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeMetricStored(name, msgHandler) {
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/metric/stored', name);
		SolarNode.WebSocket.subscribeToTopic(topic, msgHandler);
	}
	
	/* ============================
	   Init
	   ============================ */

 	queryForMetrics().always(() => {
		toggleLoading(false);
		subscribeMetricStored(undefined, handleMetricStoredMessage);
	});
});
