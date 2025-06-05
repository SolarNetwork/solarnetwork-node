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
	 * @property {number} value the value
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
	
	const aggregateMetricFilterForm = $('#metrics-aggregate-filter-form');
	const aggregateMetricFilterFromField = $('#metrics-aggregate-filter-from');
	const aggregateMetricFilterToField = $('#metrics-aggregate-filter-to');
	const aggregateMetricFilterMinField = $('#metrics-aggregate-filter-agg-min');
	const aggregateMetricFilterMaxField = $('#metrics-aggregate-filter-agg-max');
	const aggregateMetricFilterAvgField = $('#metrics-aggregate-filter-agg-avg');
	const aggregateMetricFilterSumField = $('#metrics-aggregate-filter-agg-sum');
	const aggregateMetricFilterCntField = $('#metrics-aggregate-filter-agg-cnt');
	const aggregateMetricFilterP1Check = $('#metrics-aggregate-filter-agg-p1-inc');
	const aggregateMetricFilterP1Field = $('#metrics-aggregate-filter-agg-p1');
	const aggregateMetricFilterP1Disp = $('#metrics-aggregate-filter-agg-p1-disp');
	const aggregateMetricFilterP2Check = $('#metrics-aggregate-filter-agg-p2-inc');
	const aggregateMetricFilterP2Field = $('#metrics-aggregate-filter-agg-p2');
	const aggregateMetricFilterP2Disp = $('#metrics-aggregate-filter-agg-p2-disp');

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
	
	let inputTimer = undefined;
	
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
		
		//aggregateMetricsSection.toggleClass('hidden', aggregateMetricRows.size == 0);
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
	
	const NUM_COMPONENTS = /^(-?\d+)(?:\.(\d+))?$/;

	function populateMetric(/** @type {Metric} */ metric, /** @type {number} */ row, /** @type jQuery */ itemEl) {
		itemEl.find('[data-tprop=idx]').text(row);
		itemEl.find('[data-tprop=displayTs]').text(moment(metric.timestamp).format('YYYY-MM-DD HH:mm:ss.SSS'));
		itemEl.find('[data-tprop=type]').text(metric.type);
		itemEl.find('[data-tprop=displayType]').text(displayType(metric.type));		
		itemEl.find('[data-tprop=name]').text(metric.name);
		
		const rounded = metric.value.toFixed(3);
		const comps = rounded.match(NUM_COMPONENTS);
		const whole = Number(comps[1]);
		const frac = comps[2] ? comps[2].replace(/0+$/, '') : undefined;
		
		itemEl.find('[data-tprop=valueWhole]').text(whole.toLocaleString());
		itemEl.find('[data-tprop=valueFraction]').text(frac ? frac : '');
		itemEl.find('.number-dot').toggleClass('invisible', !frac);
	}
	
	function displayType(/** @type {string} */ type) {
		let disp = aggregateMetricsSection.data('i18n-'+type.replace(':', '') );
		if ( disp ) {
			return disp;
		}
		if ( type.startsWith('q:') ) {
			return aggregateMetricsSection.data('i18n-q') + ' ' +type.substring(2) + '%';
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
	
	function aggregateListUrl(endpoint) {
		let url = SolarNode.context.path('/a/metrics/' +endpoint) 
			+ '?type=s&start='
			;

		const start = aggregateMetricFilterFromField.val();
		if (start) {
			url += moment(start).toISOString();
		} else {
			url += encodeURIComponent(moment().subtract(5, 'days').toISOString());
		}

		const end = aggregateMetricFilterToField.val();
		if (end) {
			url += '&end=' +encodeURIComponent(moment(end).add(1, 'days').toISOString());
		}

		const aggs = new Set();
		for ( let f of [aggregateMetricFilterMinField
				, aggregateMetricFilterMaxField
				, aggregateMetricFilterAvgField
				, aggregateMetricFilterSumField
				, aggregateMetricFilterCntField] ) {
			if ( f.is(":checked") ) {
				aggs.add(f.val());
			}
		}
		if ( aggregateMetricFilterP1Check.is(":checked") ) {
			aggs.add('q:' + aggregateMetricFilterP1Field.val());
		}
		if ( aggregateMetricFilterP2Check.is(":checked") ) {
			aggs.add('q:' + aggregateMetricFilterP2Field.val());
		}
		if ( aggs.size < 1 ) {
			['min', 'max', 'avg', 'q:25', 'q:75'].forEach(aggs.add, aggs);
		}
		for ( let k of aggs ) {
			url += '&aggs=' + encodeURIComponent(k);
		}
		return url;
	}

	function queryForAggregateMetrics() {
		let url = aggregateListUrl('list');
		return $.getJSON(url, (data) => {
			if ( data && data.success === true ) {
				setupAggregateMetrics(data.data);
			}
		});
	}

	function exportAggregateMetricsCsv() {
		let url = aggregateListUrl('csv');
		document.location = url;		
	}
	
	function metricsListUrl(endpoint, type, pageSize, pageOffset) {
		let url = SolarNode.context.path('/a/metrics/'+endpoint);
		url += '?offset=' + (pageOffset || '0');
		if ( type ) {
			url +=  '&type=' + type;
		}
		if (pageSize) {
			url += '&max=' + pageSize;
		}
		if ( nameInput.length > 0 ) {
			const name = nameInput.val();
			if ( name ) {
				url += '&name=' + encodeURIComponent(name);
			}
		}
		return url;
	}

	function queryForMetrics() {
		let url = metricsListUrl('list', 's', pageSize, (pageOffset * pageSize));
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
	
	function exportMetricsCsv() {
		let url = metricsListUrl('csv');
		document.location = url;		
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
		aggregateMetricsSection.toggleClass('hidden', metricRows.size < 1);
		toggleLoading(false);
		subscribeMetricStored();
	});
	
	queryForMostRecentMetrics();

	queryForAggregateMetrics();

	nameInput.on('keydown', (event) => {
		if (event.key === "Enter" ) {
			event.preventDefault();
		}
	}).on('keyup', () => {
		if (inputTimer) {
			clearTimeout(inputTimer);
		}
		inputTimer = setTimeout(() => {
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
	
	$('#metrics-list-export').on('click', exportMetricsCsv);
	$('#metrics-list-refresh').on('click', queryForMetrics);
	$('#metrics-aggregate-export').on('click', exportAggregateMetricsCsv);
	$('#metrics-aggregate-refresh').on('click', queryForAggregateMetrics)
	
	for ( let e of ['mousemove', 'touchmove'] ) {
		aggregateMetricFilterP1Field.on(e, () => {
			aggregateMetricFilterP1Disp.text(aggregateMetricFilterP1Field.val());
		}).on('change', () => {
			aggregateMetricFilterP1Check.prop('checked', true);
			aggregateMetricFilterP1Disp.text(aggregateMetricFilterP1Field.val());
		});
		aggregateMetricFilterP2Field.on(e, () => {
			aggregateMetricFilterP2Disp.text(aggregateMetricFilterP2Field.val());
		}).on('change', () => {
			aggregateMetricFilterP2Check.prop('checked', true);
			aggregateMetricFilterP2Disp.text(aggregateMetricFilterP2Field.val());
		});
	}

		aggregateMetricFilterForm.on('submit', (event) => {
		event.preventDefault();
		
		return false;
	}).find('input')
	.on('change', queryForAggregateMetrics)
	.on('keyup', () => {
		if (inputTimer) {
			clearTimeout(inputTimer);
		}
		inputTimer = setTimeout(() => {
			queryForAggregateMetrics();
		}, 500);
	});

});
