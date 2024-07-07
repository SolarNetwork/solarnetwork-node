'use strict';
SolarNode.Datum = (function() {
	var DATUM_KNOWN_PROPERTIES = Object.freeze({
		'_DatumType' : true,
		'_DatumTypes' : true,
		'created' : true,
		'event.topics' : true,
		'sourceId' : true,
	});

	var self = {};

	/**
	 * Subscribe to the "datum captured" topic.
	 *
	 * @param {string} [sourceId] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeDatumCaptured(sourceId, msgHandler) {
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/datum/captured', sourceId);
		SolarNode.WebSocket.subscribeToTopic(topic, msgHandler);
	}

	/**
	 * Subscribe to the "datum stored" topic.
	 *
	 * @param {string} [sourceId] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeDatumStored(sourceId, msgHandler) {
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/datum/stored', sourceId);
		SolarNode.WebSocket.subscribeToTopic(topic, msgHandler);
	}

	/**
	 * Subscribe to all "datum" topics.
	 *
	 * @param {string} [sourceId] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeDatum(sourceId, msgHandler) {
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/datum/*', sourceId);
		SolarNode.WebSocket.subscribeToTopic(topic, msgHandler);
	}

	/**
	 * Subscribe to all "control" topics.
	 *
	 * @param {string} [controlId] an optional source ID to subscribe to; if not provided all controls are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeControl(controlId, msgHandler) {
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/control/*', controlId);
		SolarNode.WebSocket.subscribeToTopic(topic, msgHandler);
	}

	function replaceTemplateProperties(el, obj, prefix) {
		var prop, sel;
		for ( prop in obj ) {
			if ( obj.hasOwnProperty(prop) ) {
				sel = "[data-tprop='" +(prefix || '') +prop +"']";
				el.find(sel).addBack(sel).text(obj[prop]);
			}
		}
	}

	function datumActivityForDatum(datum) {
		var activity = {
			date: moment(datum.created).format('D MMM YYYY HH:mm:ss'),
			sourceId: datum.sourceId,
			type: (datum.controlId ? 'Control' : datum._DatumType ? datum._DatumType.replace(/^.*\./, '') : 'N/A'),
			event: datum['event.topics'].replace(/^.*\//, ''),
			props: [],
		};
		var prop;
		for ( prop in datum ) {
			if ( datum.hasOwnProperty(prop) && !DATUM_KNOWN_PROPERTIES.hasOwnProperty(prop) ) {
				activity.props.push({propName:prop, propValue:datum[prop]});
			}
		}
		return activity;
	}

	function iconNameForActivity(activity) {
		var eventName = activity.event;
		if ( eventName === 'DATUM_STORED' ) {
			return 'bi bi-hdd';
		} else if ( eventName === 'DATUM_CAPTURED'
				|| eventName === 'CONTROL_INFO_CAPTURED'
				|| eventName === 'CONTROL_INFO_CHANGED' ) {
			return 'bi bi-plus-lg';
		} else if ( eventName === 'DATUM_UPLOADED' ) {
			return 'bi bi-cloud-arrow-up';
		}
		return null;
	}

	function addDatumActivityTableRow(tbody, templateRow, activity) {
		var tr = templateRow.clone(true),
			propList,
			propListTemplateRow,
			propLi,
			iconName = iconNameForActivity(activity);
		tr.removeClass('template');
		tr.data('activity', activity);
		replaceTemplateProperties(tr, activity);
		if ( iconName ) {
			tr.find('.event-icon').addClass(iconName).removeClass('hidden').attr('title', activity.event);
		}
		if ( activity.props.length > 0 ) {
			propList = tr.find('.datum-props');
			propListTemplateRow = propList.find('.datum-prop.template');
			activity.props.forEach(function(datumProp) {
				propLi = propListTemplateRow.clone(true);
				propLi.removeClass('template');
				replaceTemplateProperties(propLi, datumProp);
				propList.append(propLi);
			});
			propListTemplateRow.remove();
			propList.removeClass('hidden');
		}
		tbody.prepend(tr);
	}

	function updateDatumActivitySeenPropsTableRow(tbody, templateRow, activity) {
		var tr = tbody.find('.row.activity').filter(function() {
				var d = $(this).data('activity');
				return (d && d.sourceId === activity.sourceId);
			}),
			propList,
			propListTemplateRow,
			propLi;

		if ( tr.length < 1 ) {
			tr = templateRow.clone(true);
			tr.removeClass('template');
			tr.data('activity', activity);
		} else {
			tr.removeClass('brief-showcase');
		}
		replaceTemplateProperties(tr, activity);
		if ( activity.props.length > 0 ) {
			propList = tr.find('.datum-props');
			propListTemplateRow = propList.find('.datum-prop.template');
			activity.props.forEach(function(datumProp) {
				propLi = propList.find('.datum-prop').filter(function() {
					var n = $(this).data('prop-name');
					return (n === datumProp.propName);
				});
				if ( propLi.length < 1 ) {
					propLi = propListTemplateRow.clone(true);
					propLi.removeClass('template');
					propLi.data('prop-name', datumProp.propName);
				}
				replaceTemplateProperties(propLi, datumProp);
				if ( !$.contains(document.documentElement, propLi.get(0)) ) {
					propList.append(propLi);
				}
			});
			propList.removeClass('hidden');
		}
		if ( !$.contains(document.documentElement, tr.get(0)) ) {
			tbody.append(tr);
		} else {
			setTimeout(function() {
				// kick to another event loop
				tr.addClass('brief-showcase');
			}, 100);
		}
	}

	function updateExternalNodeLinks(container) {
		$.getJSON(SolarNode.context.path('/a/config')).then(function(json) {
			var replaced = false;
			if ( json && json.data && json.data.networkServiceUrls ) {
				var urls = json.data.networkServiceUrls;
				Object.keys(urls).forEach(function(name) {
					var url = urls[name].replace(/\{nodeId\}/, SolarNode.nodeId);
					var els = container.find('.'+name);
					if ( els.length > 0 ) {
						els.find('a.external-link').attr('href', url);
						els.removeClass('hidden');
						replaced = true;
					}
				});
			}
			if ( replaced ) {
				container.removeClass('hidden');
			}
		});
	}

	return Object.defineProperties(self, {
		subscribeControl : { value : subscribeControl },

		subscribeDatum : { value : subscribeDatum },
		subscribeDatumCaptured : { value : subscribeDatumCaptured },
		subscribeDatumStored : { value : subscribeDatumStored },

		datumActivityForDatum : { value : datumActivityForDatum },
		addDatumActivityTableRow : { value : addDatumActivityTableRow },
		updateDatumActivitySeenPropsTableRow : { value : updateDatumActivitySeenPropsTableRow },

		updateExternalNodeLinks : { value : updateExternalNodeLinks },
	});
}());

$(document).ready(function() {
	if ( !SolarNode.isAuthenticated() ) {
		return;
	}

	$('#datum-activity').first().each(function() {
		var activityContainer = $(this),
			activityTableBody = activityContainer.find('.activity-container'),
			activityTableTemplateRow = activityContainer.find('.activity.template');
		var seenPropsContainer = $('#datum-activity-seenprops'),
			seenPropsTableBody = seenPropsContainer.find('.activity-container'),
			seenPropsTableTemplateRow = seenPropsContainer.find('.activity.template');
		var revealed = false;

		var handler = function handleMessage(msg) {
			var datum = JSON.parse(msg.body).data,
				activity = SolarNode.Datum.datumActivityForDatum(datum);
			console.info('Got %o message: %o', activity.event, activity);

			SolarNode.Datum.addDatumActivityTableRow(activityTableBody, activityTableTemplateRow, activity);
			activityTableBody.find('>*:gt(9)').remove();
			if ( activity.event !== 'DATUM_UPLOADED' ) {
				SolarNode.Datum.updateDatumActivitySeenPropsTableRow(seenPropsTableBody, seenPropsTableTemplateRow, activity);
			}
			if ( !revealed ) {
				activityContainer.removeClass('hidden');
				seenPropsContainer.removeClass('hidden');
			}
		};

		SolarNode.Datum.subscribeDatum(null, handler);
		SolarNode.Datum.subscribeControl(null, handler);

	});

	$('#external-node-links').first().each(function() {
		var container = $(this);
		SolarNode.Datum.updateExternalNodeLinks(container);
	});
});
