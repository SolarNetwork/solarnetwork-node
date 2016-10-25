var OCPPKiosk = (function() {
	'use strict';
	
	var $ = document.querySelector.bind(document);
	var self = {};
	
	var context = (function() {
		var basePath = undefined;
		
		var contextPath = function() {
			if ( basePath === undefined ) {
				basePath = $('meta[name=base-path]').getAttribute('content');
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

	function formatPower(power) {
		var k = (Number.isFinite(power) ? power : 0) / 1000;
		return k.toFixed(1);
	}
	
	function formatEnergy(energy) {
		return formatPower(energy);
	}
		
	function formatDuration(ms, unitEl) {
		var mins = (Number.isFinite(ms) ? ms : 0) / 60000;
		if ( mins > 59.5 ) {
			if ( unitEl ) {
				unitEl.innerText = 'hr';
			}
			return (mins / 60).toFixed(1);
		}
		if ( unitEl ) {
			unitEl.innerText = 'min';
		}
		return mins.toFixed(0);
	}
	
	function refreshPvData(data) {
		var el = $('#pv-power');
		if ( el ) {
			el.innerText = formatPower(data.power);
		}
	}
	
	function refreshSocketData(data, key) {
		var cssKey = key.toLowerCase(),
			statusEl = $('#socket-status-' +cssKey),
			powerEl = $('#socket-power-' +cssKey),
			energyEl = $('#socket-energy-' +cssKey),
			durEl = $('#socket-duration-' +cssKey),
			durUnitEl = $('#socket-duration-unit-' +cssKey);
		statusEl.classList.toggle('active', data.endDate === 0);
		if ( powerEl ) {
			powerEl.innerText = formatPower(data.power);
		}
		if ( energyEl ) {
			energyEl.innerText = formatEnergy(data.energy);
		}
		if ( durEl ) {
			durEl.innerText = formatDuration(data.duration, durUnitEl);
		}
	}
	
	function refreshGridData(data) {
		var el = $('#grid-power');
		if ( el ) {
			el.innerText = formatPower(Math.abs(data.power));
		}
	}
	
	function processKioskDataUpdate(data) {
		console.log('Got kisok data: %s', JSON.stringify(data));
		var key, 
			pvProd = 0,
			gridDraw = 0,
			socketTotalDraw = null,
			socketDraw = { A:null, B:null },
			gif = $('#ocpp-state'),
			gifName;
		if ( data.pvData ) {
			refreshPvData(data.pvData);
			if ( data.pvData.power ) {
				pvProd = data.pvData.power;
			}
		}
		if ( data.socketData ) {
			for ( key in data.socketData ) {
				refreshSocketData(data.socketData[key], key);
				if ( data.socketData[key].power !== undefined ) {
					socketDraw[key] = data.socketData[key].power;
					socketTotalDraw += socketDraw[key];
				}
			}
			
			// turn off any socket we didn't get data for
			for ( key in socketDraw ) {
				if ( socketDraw[key] === null ) {
					refreshSocketData({}, key)
				}
			}
		}
		gridDraw = (socketTotalDraw - pvProd);
		refreshGridData({power:gridDraw});
		if ( pvProd < 1 && socketTotalDraw === null ) {
			// nothing going on
			gifName = 'Off';
		} else if ( pvProd > 0 && socketTotalDraw === null ) {
			gifName = 'Export-Only';
		} else {
			if ( socketDraw.A !== null && socketDraw.B !== null ) {
				gifName = 'AB';
			} else if ( socketDraw.A !== null ) {
				gifName = 'A';
			} else {
				gifName = 'B';
			}
			if ( pvProd > 0 && gridDraw > 0 ) {
				gifName += '1';
			} else if ( pvProd > 0 && gridDraw === 0 ) {
				gifName += '2';
			} else if ( pvProd < 1 && gridDraw > 0 ) {
				gifName += '3';
			} else {
				// exporting
				gifName += '4';
			}
		}
		gifName = 'img/'+gifName+'.gif';
		if ( !gif.src.endsWith(gifName) ) {
			gif.src = gifName;
		}
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
	
	function mockCycle() {
		var max = 8, 
			digits = 2,
			r = (Math.floor(Math.random() * max) +1),
			path = ('/data/mock-'+(function() {
				var s = ''+r;
				while ( s.length < digits ) {
					s = '0' +s;
				}
				return s;
			}()) + '.json');
		reqJSON({path : path}).then(function(data) {
			processKioskDataUpdate(data)
			setTimeout(mockCycle, 2000);
		}, function(error) {
			setTimeout(mockCycle, 2000);
		});
	}
	
	websocketConnect();
	mockCycle();
	return self;
})();
