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
	 * @property {number} totalResults the total result count
	 * @property {number} returnedResultCount the number of results returned
	 * @property {number} startingOffset the starting offset
	 * @property {Metric[]} results the metrics
	 */
	
	const i18nPage = document.getElementById('metrics').dataset.i18nPage;

	const mostRecentMetricsSection = $('#metrics-most-recent');
	const mostRecentMetricTemplate = mostRecentMetricsSection.find('.template');
	const mostRecentMetricContainer = mostRecentMetricsSection.find('.list-container');
	
	const aggregateMetricsSection = $('#metrics-aggregate');
	const aggregateMetricTemplate = aggregateMetricsSection.find('.template');
	const aggregateMetricContainer = aggregateMetricsSection.find('.list-container');

	const metricTemplate = $('#metrics-list .template');
	const metricContainer = $('#metrics-list .list-container');
	
	const nameInput = $('#metrics-list-filter-name');
	const prevPageBtn = $('#metrics-list-nav-prev');
	const selectPageBtn = $('#metrics-list-nav-menu');
	const selectPageContainer = $('#metrics-list-nav-menu-page-container');
	const nextPageBtn = $('#metrics-list-nav-next');

	/** @type Map<String, jQuery> */
	const mostRecentMetricRows = new Map();

	/** @type Map<String, jQuery> */
	const aggregateMetricRows = new Map();

	/** @type Map<String, jQuery> */
	const metricRows = new Map();
	
	let pageSize = 10;
	let totalPageCount = 0;
	let pageOffset = 0;
	
	let nameInputTimer = undefined;
	
	function setupMostRecentMetrics(/** @type {MetricFilterResults} */ metrics) {
		mostRecentMetricRows.clear();
		mostRecentMetricContainer.empty();
		
		if ( Array.isArray(metrics.results) ) {
			for ( let metric of metrics.results ) {
				renderMetric(metric, 0, mostRecentMetricTemplate, mostRecentMetricContainer, mostRecentMetricRows, mostRecentMetricKey(metric));
			}
		}
		
		mostRecentMetricsSection.toggleClass('hidden', mostRecentMetricRows.size == 0);
	}

	function setupAggregateMetrics(/** @type {MetricFilterResults} */ metrics) {
		aggregateMetricRows.clear();
		aggregateMetricContainer.empty();
		
		if ( Array.isArray(metrics.results) ) {
			for ( let metric of metrics.results ) {
				renderMetric(metric, 0, aggregateMetricTemplate, aggregateMetricContainer, aggregateMetricRows, mostRecentMetricKey(metric));
			}
		}
		
		aggregateMetricsSection.toggleClass('hidden', aggregateMetricRows.size == 0);
	}

	function setupMetrics(/** @type {MetricFilterResults} */ metrics) {
		metricRows.clear();
		metricContainer.empty();
		
		if ( Array.isArray(metrics.results) ) {
			let row = metrics.startingOffset;
			for ( let metric of metrics.results ) {
				renderMetric(metric, ++row, metricTemplate, metricContainer, metricRows, metricKey(metric));
			}
		}
			
		prevPageBtn.prop('disabled', pageOffset < 1);
		nextPageBtn.prop('disabled', (pageOffset + 1) * pageSize > metrics.totalResults);

		let pageCount = Math.ceil(metrics.totalResults / pageSize);
		if ( pageCount != totalPageCount ) {
			// (re)render page links
			selectPageContainer.empty();
			for ( let i = 0; i < pageCount; i++ ) {
				const btn = $('<button class="dropdown-item" type="button">').text(`${i18nPage} ${i +1}`);
				btn.data('page', i);
				if ( i == pageOffset ) {
					btn.addClass('active');
				}
				btn.on('click', pageMenuJump);
				selectPageContainer.append($('<li>').append(btn));
			}
			totalPageCount = pageCount;
		}
		
		selectPageBtn.prop('disabled', pageCount < 1);
	}
	
	function pageMenuJump(event) {
		const btn = $(event.target);
		const page = btn.data('page');
		if (page != pageOffset) {
			pageOffset = page;
			selectPageContainer.find('button.active').removeClass('active');
			btn.addClass('active');
			queryForMetrics();
		}
	}

	function toggleLoading(on) {
		$('.init').toggleClass('hidden', !on);
		$('.ready').toggleClass('hidden', on);
	}
	
	function renderMetric(/** @type {Metric} */ metric
			, /** @type {number} */ row
			, /** @type {jQuery} */ template
			, /** @type {jQuery} */ dest
			, /** @type {Map<String, jQuery>} */ rowMap
			, /** @type {string} */ rowKey) {
		const itemEl = template.clone(true).removeClass('template');
		populateMetric(metric, row, itemEl);
		dest.append(itemEl);
		rowMap.set(rowKey, itemEl);
	}

	function populateMetric(/** @type {Metric} */ metric, /** @type {number} */ row, /** @type jQuery */ itemEl) {
		itemEl.find('[data-tprop=idx]').text(row);
		itemEl.find('[data-tprop=displayTs]').text(moment(metric.timestamp).format('YYYY-MM-DD HH:mm:ss.SSS'));
		itemEl.find('[data-tprop=type]').text(metric.type);
		itemEl.find('[data-tprop=displayType]').text(displayType(metric.type));		
		itemEl.find('[data-tprop=name]').text(metric.name);
		itemEl.find('[data-tprop=value]').text(metric.value);
	}
	
	function displayType(/** @type {string} */ type) {
		let disp = aggregateMetricsSection.data('i18n-'+type.replace(':', '') );
		if ( disp ) {
			return disp;
		}
		return type;
	}
	
	function mostRecentMetricKey(metric) {
		if (!metric ) {
			return undefined;
		}
		return `${metric.type}.${metric.name}`;
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

		const key = mostRecentMetricKey(metric);
		const itemEl = mostRecentMetricRows.get(key);
		if ( !itemEl ) {
			renderMetric(metric, 0, mostRecentMetricTemplate, mostRecentMetricContainer, mostRecentMetricRows, key);
		} else {
			itemEl.removeClass('brief-showcase');
			populateMetric(metric, 0, itemEl);
			setTimeout(() => {
				// kick to another event loop
				itemEl.addClass('brief-showcase');
			}, 100);
		}
	}
	
	function queryForMostRecentMetrics() {
		let url = SolarNode.context.path('/a/metrics/list') 
			+ '?type=s&mostRecent=true&'
			+ encodeURIComponent('sorts[0].sortKey') +'=name'
			;
		return $.getJSON(url, (data) => {
			if ( data && data.success === true ) {
				setupMostRecentMetrics(data.data);
			}
		});
	}

	function queryForAggregateMetrics() {
		let url = SolarNode.context.path('/a/metrics/list') 
			+ '?type=s&start='
			+ encodeURIComponent(moment().subtract(5, 'days').format());
			;
		for ( let k of ['min', 'max', 'avg', 'q:25', 'q:75'] ) {
			url += '&aggs=' + encodeURIComponent(k);
		}
		return $.getJSON(url, (data) => {
			if ( data && data.success === true ) {
				setupAggregateMetrics(data.data);
			}
		});
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
			}
		});
	}

	function subscribeMetricStored() {
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/metric/stored');
		SolarNode.WebSocket.subscribeToTopic(topic, handleMetricStoredMessage);
	}
	
	function activateSelectMenuItem(idx) {
		selectPageContainer.find('button.active').removeClass('active');
		selectPageContainer.find('button:eq('+idx+')').addClass('active');
	}
	
	/* ============================
	   Init
	   ============================ */

 	queryForMetrics().always(() => {
		$('#metrics .none').toggleClass('hidden', metricRows.size > 0);
		$('#metrics .some').toggleClass('hidden', metricRows.size < 1);
		toggleLoading(false);
		subscribeMetricStored();
	});
	
	queryForMostRecentMetrics();

	queryForAggregateMetrics();

	nameInput.on('keydown', (event) => {
		if (event.key === "Enter" ) {
			event.preventDefault();
		}
	}).on('keyup', (event) => {
		if (nameInputTimer) {
			clearTimeout(nameInputTimer);
		}
		nameInputTimer = setTimeout(() => {
			pageOffset = 0;
			queryForMetrics();
		}, 500);
	});
	
	prevPageBtn.on('click', () => {
		pageOffset--;
		activateSelectMenuItem(pageOffset);
		queryForMetrics();
	});

	nextPageBtn.on('click', () => {
		pageOffset++;
		activateSelectMenuItem(pageOffset);
		queryForMetrics();
	});
	
	$('#metrics-list-refresh').on('click', queryForMetrics);
});
