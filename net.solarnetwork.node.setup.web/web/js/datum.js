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
	var stompClient;
	var subscriptions = [];
	
	function doWithConnection(callback) {
		if ( stompClient != null ) {
			callback(null, stompClient);
		}
		var csrf = SolarNode.csrfData;
		var url = 'ws://' +document.location.host +SolarNode.context.path('/ws');
		var socket = new WebSocket(url);
		var client = Stomp.over(socket);
		client.debug = true;
		var headers = {};
		headers[csrf.headerName] = csrf.token;
		client.connect(headers, function(frame) {
			stompClient = client;
			console.info('Connected to node WebSocket');
			callback(null, client);
		}, function (error) {
			console.error('STOMP protocol error %s', error);
			callback(error);
		});
	}
	
	function subscribeToTopic(topic, msgHandler) {
		doWithConnection(function(err, client) {
			if ( err ) {
				return;
			}
			var sub = client.subscribe(topic, msgHandler);
			if ( sub ) {
				console.info('Subscribed to message topic %s', topic);
				subscriptions.push(sub);
			}
		});
	}
	
	function topicNameWithWildcardSuffix(topic, suffix) {
		var topicSuffix = (suffix ? suffix : '*');
		if ( !topicSuffix.startsWith('/') ) {
			topic += '/';
		}
		topic += topicSuffix;
		return topic;
	}
	
	/**
	 * Subscribe to the "datum created" topic.
	 * 
	 * @param {string} [sourceId] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeDatumCreated(sourceId, msgHandler) {
		var topic = topicNameWithWildcardSuffix('/topic/datum/created', sourceId);
		subscribeToTopic(topic, msgHandler);
	}
	
	/**
	 * Subscribe to the "datum stored" topic.
	 * 
	 * @param {string} [sourceId] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeDatumStored(sourceId, msgHandler) {
		var topic = topicNameWithWildcardSuffix('/topic/datum/stored', sourceId);
		subscribeToTopic(topic, msgHandler);
	}

	/**
	 * Subscribe to all "datum" topics.
	 * 
	 * @param {string} [sourceId] an optional source ID to subscribe to; if not provided all sources are subscribed
	 * @param {function} msgHandler the callback function that accepts error and message arguments
	 */
	function subscribeDatum(sourceId, msgHandler) {
		var topic = topicNameWithWildcardSuffix('/topic/datum/*', sourceId);
		subscribeToTopic(topic, msgHandler);
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
			type: datum._DatumType.replace(/^.*\./, ''),
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

	function addDatumActivityTableRow(tbody, templateRow, activity) {
		var tr = templateRow.clone(true),
			propList,
			propListTemplateRow,
			propLi;
		tr.removeClass('template');
		tr.data('activity', activity);
		replaceTemplateProperties(tr, activity);
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
			propList.removeClass('hide');
		}
		tbody.prepend(tr);
	}
	
	return Object.defineProperties(self, {
		subscribeDatum : { value : subscribeDatum },
		subscribeDatumCreated : { value : subscribeDatumCreated },
		subscribeDatumStored : { value : subscribeDatumStored },
		
		datumActivityForDatum : { value : datumActivityForDatum },
		addDatumActivityTableRow : { value : addDatumActivityTableRow },
	});
}());

$(document).ready(function() {
	if ( !SolarNode.isAuthenticated() ) {
		return;
	}
	
	$('#datum-activity').first().each(function() {
		$(this).removeClass('hide');
		var tbody = $(this).find('table.datum-activity tbody'),
			templateRow = $(this).find('table.datum-activity tr.template');
		SolarNode.Datum.subscribeDatum(null, function(msg) {
			var datum = JSON.parse(msg.body).data,
				activity = SolarNode.Datum.datumActivityForDatum(datum);
			console.info('Got %o message: %o', activity.event, activity);
			SolarNode.Datum.addDatumActivityTableRow(tbody, templateRow, activity);
			tbody.find('tr:gt(9)').remove();
		});
	});
});
