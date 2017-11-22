'use strict';
SolarNode.Platform = (function() {
	var self = {};
	var subscribedToTask = false;
	var waitingForRestart = false;
	
	function getActivePlatformTaskInfo(callback) {
		$.getJSON(SolarNode.context.path('/pub/platform/task'), function(json) {
			callback(json && json.data ? json.data : null);
		}).fail(function() {
			callback(null);
		});
	}
	
	function updatePlatformTaskInfo(modal, info) {
		modal.find('.info-title').text(info.title || '');
		modal.find('.info-message').text(info.message || '');
		modal.find('.bar').css('width', Math.round((+info.percentComplete || 0) * 100) + '%');
		modal.find('.restart-required').toggleClass('hide', !info.restartRequired);
	}
	
	function setPlatformLockModalVisible(modal, visible) {
		modal.modal(visible ? 'show' : 'hide');
	}
	
	function handleRestart(modal) {
		if ( waitingForRestart ) {
			return;
		}
		waitingForRestart = true;
		modal.find('.bar').css('width', '100%');
		modal.find('.restarting').removeClass('hide');
		modal.find('.hide-while-restarting').addClass('hide');
		setTimeout(function() {
			SolarNode.tryGotoURL(SolarNode.context.path('/a/home'));
		}, 10000);
		setPlatformLockModalVisible(modal, true);
	}

	function subscribeToTask(modal) {
		if ( subscribedToTask ) {
			return;
		}
		SolarNode.WebSocket.subscribeToTopic('/pub/topic/platform/task', function(msg) {
			var body = (msg && msg.body ? JSON.parse(msg.body) : null);
			var info = (body ? body.data : null);
			if ( info ) {
				updatePlatformTaskInfo(modal, info);
			}
			if ( info && info.complete === false ) {
				setPlatformLockModalVisible(modal, true);
			}
		}, {
			"Accept-Language" : (navigator.language || navigator.userLanguage || 'en')
		});
		subscribedToTask = true;
		getActivePlatformTaskInfo(function(info) {
			if ( info ) {
				updatePlatformTaskInfo(modal, info);
				setPlatformLockModalVisible(modal, true);
			}
		});
	}
	
	function checkPlatformState(modal) {
		var checkStateUrl = modal.attr('action');
		
		$.getJSON(checkStateUrl, function(data) {
			if ( data === undefined || data.success !== true ) {
				SolarNode.warn('Error!', 'An error occured checking the platform state.');
				return;
			}
			var lockActive = data.data === 'UserBlockingSystemTask';
			if ( lockActive ) {
				subscribeToTask(modal);
			} else {
				setPlatformLockModalVisible(modal, false);
			}
		}).fail(function(err) {
			console.warn('An error occured checking the platform state at %s: %s', checkStateUrl, err);
		});
	}

	return Object.defineProperties(self, {
		checkPlatformState : { value : checkPlatformState },
		setPlatformLockModalVisible : { value : setPlatformLockModalVisible },
		subscribeToTask : { value : subscribeToTask },
		handleRestart : { value : handleRestart },
	});
}());

$(document).ready(function() {
	$('#platform-lock-modal').first().each(function() {
		var modal = $(this);
		
		function handlePlatformStateMessage(message) {
			var msg = JSON.parse(message.body);
			if ( msg.data ) {
				var currState = msg.data.platformState;
				console.info('Got platform/state change from %s \u2192 %s', msg.data.oldPlatformState, currState);
				var lockActive = msg.data.platformState === 'UserBlockingSystemTask';
				if ( currState === 'UserBlockingSystemTask' ) {
					SolarNode.Platform.subscribeToTask(modal);
				} else if ( currState === 'Restarting' ) {
					SolarNode.Platform.handleRestart(modal);
				} else {
					SolarNode.Platform.setPlatformLockModalVisible(modal, false);
				}
			}
		};

		// subscribe for state change messages
		SolarNode.WebSocket.subscribeToTopic('/pub/topic/platform/state', handlePlatformStateMessage);
		
		// check the current state
		SolarNode.Platform.checkPlatformState(modal);
	});
});
