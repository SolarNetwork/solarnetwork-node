'use strict';
SolarNode.WebSocket = (function() {
	var self = {};
	var stompClient;
	var pingTask;
	var subscriptions = {};
	var subscriptionHandlers = {};
	var connectTasks = [];
	var connecting = false;
	
	function executePing(url) {
		$.getJSON(SolarNode.context.path('/csrf')).then(subscribeDisconnectedTopics);
	}
	
	function doWithConnection(callback) {
		if ( stompClient != null ) {
			callback(null, stompClient);
		}
		// so we don't try to fire up multiple WebSocket instances from multiple connection tasks,
		// stash tasks on array so we can invoke the task callback(s) later once connected
		connectTasks.push({callback:callback});

		// if we've already invoked connect() but are waiting for the response, we're done 
		// because this task will be executed in the connection callback
		if ( connecting ) {
			return;
		}

		connect();
	}

	function connect() {
		connecting = true;

		var csrf = SolarNode.csrfData;
		var url = (document.location.protocol === 'https:' ? 'wss://' : 'ws://') 
			+document.location.host +SolarNode.context.path('/ws');
		var socket = new WebSocket(url);
		var client = Stomp.over(socket);
		client.debug = false;
		var headers = {};
		headers[csrf.headerName] = csrf.token;

		client.connect(headers, function(frame) {
			console.info('Connected to node WebSocket');
			stompClient = client;
			try {
				connectTasks.forEach(function(task) {
					if ( typeof task.callback === 'function' ) {
						task.callback(null, client);
					}
				});

				// check for subscriptions that need to be re-subscribed
				subscribeDisconnectedTopics();
			} finally {
				connecting = false;
			}
		}, function (error) {
			console.error('STOMP protocol error %s', error);
			subscriptions = {};
			stompClient = null;

			// reconnect automatically
			setTimeout(connect, 10000);
		});

		if ( !pingTask ) {
			// create a "ping" HTTP request that helps keep the WebSocket alive
			// and can re-connect and re-subscribe if the connection drops later
			pingTask = setInterval(executePing, 60000);
		}
	}

	function subscribeDisconnectedTopics() {
		var topic, config;
		for ( topic in subscriptionHandlers ) {
			config = subscriptionHandlers[topic];
			if ( config && !subscriptions[topic] ) {
				console.info('Re-subscribing to topic ' + topic +'...');
				doWithConnection(function(err, client) {
					if ( err ) {
						return;
					}
					doSubscribeToTopic(client, topic, config.handler, config.headers);
				});
			}
		}
	}
	
	function doSubscribeToTopic(client, topic, msgHandler, headers) {
		var sub = client.subscribe(topic, msgHandler, headers);
		if ( sub ) {
			console.info('Subscribed to message topic %s', topic);
			subscriptions[topic] = sub;
		} else {
			console.error('Unable to subscribe to topic ' +topic);
		}
	}
	
	function subscribeToTopic(topic, msgHandler, headers) {
		console.debug('Subscribing to topic ' +topic +'...');
		subscriptionHandlers[topic] = {handler:msgHandler, headers:headers};
		doWithConnection(function(err, client) {
			if ( err ) {
				console.error('Unable to get WebSocket connection to subscribe to topic ' +topic +': ' +err);
				return;
			}
			doSubscribeToTopic(client, topic, msgHandler, headers);
		});
	}
	
	function unsubscribeFromTopic(topic) {
		var sub = subscriptions[topic];
		if ( sub ) {
			sub.unsubscribe();
			delete subscriptions[topic];
			delete subscriptionHandlers[topic];
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
