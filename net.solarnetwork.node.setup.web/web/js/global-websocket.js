'use strict';
SolarNode.WebSocket = (function() {
	var self = {};
	var stompClient;
	var pingTask;
	var subscriptions = {};
	
	function executePing(url) {
		$.getJSON(SolarNode.context.path('/csrf')).then(function() {
			// nothing, just used to keep the socket connection alive
		});
	}
	
	function doWithConnection(callback) {
		if ( stompClient != null ) {
			callback(null, stompClient);
		}
		var csrf = SolarNode.csrfData;
		var url = (document.location.protocol === 'https:' ? 'wss://' : 'ws://') 
			+document.location.host +SolarNode.context.path('/ws');
		var socket = new WebSocket(url);
		var client = Stomp.over(socket);
		client.debug = false;
		var headers = {};
		headers[csrf.headerName] = csrf.token;
		client.connect(headers, function(frame) {
			stompClient = client;
			console.info('Connected to node WebSocket');
			
			if ( !pingTask ) {
				pingTask = setInterval(executePing, 60000);
			}
			
			callback(null, client);
		}, function (error) {
			console.error('STOMP protocol error %s', error);
			callback(error);
		});
	}
	
	function subscribeToTopic(topic, msgHandler, headers) {
		doWithConnection(function(err, client) {
			if ( err ) {
				return;
			}
			var sub = client.subscribe(topic, msgHandler, headers);
			if ( sub ) {
				console.info('Subscribed to message topic %s', topic);
				subscriptions[topic] = sub;
			}
		});
	}
	
	function unsubscribeFromTopic(topic) {
		var sub = subscriptions[topic];
		if ( sub ) {
			sub.unsubscribe();
			delete subscriptions[topic];
		}
	}
	
	function sendMessage(destination, headers, body) {
		doWithConnection(function(err, client) {
			if ( err ) {
				return;
			}
			client.send(destination, headers, body);
		});
	}
	
	function topicNameWithWildcardSuffix(topic, suffix) {
		var topicSuffix = (suffix ? suffix : '**');
		if ( !topicSuffix.startsWith('/') ) {
			topic += '/';
		}
		topic += topicSuffix;
		return topic;
	}
	
	
	return Object.defineProperties(self, {
		doWithConnection : { value : doWithConnection },

		sendMessage : { value : sendMessage },
		
		subscribeToTopic : { value : subscribeToTopic },
		unsubscribeFromTopic : { value : unsubscribeFromTopic },
		
		topicNameWithWildcardSuffix : { value : topicNameWithWildcardSuffix },
	});
}());
