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

	const metricTemplate = $('#metrics-list .template');
	const metricContainer = $('#metrics-list .list-container');
	
	const nameInput = $('#metrics-list-filter-name');
	const prevPageBtn = $('#metrics-list-nav-prev');
	const selectPageBtn = $('#metrics-list-nav-menu');
	const selectPageContainer = $('#metrics-list-nav-menu-page-container');
	const nextPageBtn = $('#metrics-list-nav-next');

	const metricRows = new Map();
	
	let pageSize = 10;
	let totalPageCount = 0;
	let pageOffset = 0;
	
	let nameInputTimer = undefined;
	
	function setupMetrics(/** @type {MetricFilterResults} */ metrics) {
		metricRows.clear();
		metricContainer.empty();
		
		if ( Array.isArray(metrics.results) ) {
			let row = metrics.startingOffset;
			for ( let metric of metrics.results ) {
				renderMetric(metric, ++row);
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

	nameInput.on('keyup', () => {
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
});
