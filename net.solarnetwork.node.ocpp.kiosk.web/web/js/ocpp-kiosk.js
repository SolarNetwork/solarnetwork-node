var OCPPKiosk = (function() {
	'use strict';
	
	var self = {};
	
	var context = (function() {
		var basePath = undefined;
		
		var contextPath = function() {
			if ( basePath === undefined ) {
				basePath = document.querySelector('meta[name=base-path]').getAttribute('content');
			}
			return (basePath === undefined ? "" : basePath);
		};
		
		var helper = {
			
			basePath : contextPath,
			
			path : function(path) {
				var p = contextPath();
				var p1 = String(path);
				if ( p.search(/\/$/) >= 0 && p1.length > 0 && p1.charAt(0) === '/' ) {
					p += p1.substring(1);
				} else {
					p += p1;
				}
				return p;
			}
			
		};
		
		return helper;
	})();
	
	function reqJSON(req) {
		return new Promise(function(resolve, reject) {
			var xhr, h;
			
		    if ( !req.method ) {
		    	req.method = 'GET';
		    }
		    if ( !req.path ) {
		    	req.path = '';
		    }
		    if ( !req.url ) {
		    	req.url = context.path(req.path);
		    }
	
		    xhr = new XMLHttpRequest();
		    xhr.onload = function() {
				if ( this.status >= 200 && this.status < 300 ) {
					resolve(JSON.parse(this.responseText));
				} else {
					reject(this.statusText);
				}
			};
			xhr.onerror = function() {
	          reject(this.statusText);
	        };
	
			xhr.open(req.method, req.url);
			if ( req.headers ) {
				for ( h in req.headers ) {
					xmlhttp.setRequestHeader(h, req.headers[h]);
				}
			}
	
			xhr.send(req.body);
		});
	  }


	function processKioskDataUpdate(data) {
		console.log('Got kisok data: %s', JSON.stringify(data));
	}

	function websocketConnect() {
		reqJSON({method : 'GET', url : '/csrf'}).then(function(csrf) {
			// save the CSRF for later
			self.csrf = csrf;
			
			// connect to websocket
			var url = 'ws://' +document.location.host +'/ws';
			var socket = new WebSocket(url);
			var client = Stomp.over(socket);
			//client.debug = null;
			var headers = {};
			headers[csrf.headerName] = csrf.token;
			client.connect(headers, function(frame) {
				// subscribe to /pub/topic/ocpp/kiosk to get notified of updates
				client.subscribe('/pub/topic/ocpp/kiosk', function(message) {
					if ( message.body ) {
						processKioskDataUpdate(JSON.parse(message.body));
					}
				});
			}, function (error) {
				console.log('STOMP protocol error: %s (will attempt to reconnect)', error);
				setTimeout(websocketConnect, 2000);
			});
		}, function(error) {
			console.log('Failed to obtain CSRF token: %s (will attempt to reconnect)', error);
			setTimeout(websocketConnect, 2000);
		});
	}
	
	websocketConnect();
	return self;
})();
