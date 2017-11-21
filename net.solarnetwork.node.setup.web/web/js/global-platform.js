'use strict';
SolarNode.Platform = (function() {
	var self = {};
	var willBeRestarting = false;
	
	function getActivePlatformTaskInfo(callback) {
		$.getJSON(SolarNode.context.path('/platform/task'), function(json) {
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

	function refreshPlatformTaskInfo(modal) {
		function handleRestart() {
			modal.find('.bar').css('width', '100%');
			modal.find('.restarting').removeClass('hide');
			modal.find('.hide-while-restarting').addClass('hide');
			setTimeout(function() {
				SolarNode.tryGotoURL(SolarNode.context.path('/a/home'));
			}, 10000);
		}
		function refresh() {
			getActivePlatformTaskInfo(function(info) {
				if ( info ) {
					updatePlatformTaskInfo(modal, info);
				}
				if ( info && info.complete === false ) {
					willBeRestarting = info.restartRequired;
					setPlatformLockModalVisible(modal, true);
					setTimeout(refresh, 10000);
				} else if ( willBeRestarting ) {
					if ( info ) {
						setTimeout(refresh, 10000);
					} else {
						handleRestart();
					}
				} else {
					setPlatformLockModalVisible(modal, false);
				}
			});
		}
		refresh();
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
				refreshPlatformTaskInfo(modal);
			} else {
				setPlatformLockModalVisible(modal, false);
			}
		});
	}

	return Object.defineProperties(self, {
		willBeRestarting : { get : function() { return willBeRestarting; } },

		checkPlatformState : { value : checkPlatformState },
		setPlatformLockModalVisible : { value : setPlatformLockModalVisible },
		refreshPlatformTaskInfo : { value : refreshPlatformTaskInfo },
	});
}());

$(document).ready(function() {
	$('#platform-lock-modal').first().each(function() {
		var modal = $(this);
		
		function handlePlatformStateMessage(msg) {
			var msg = JSON.parse(msg.body);
			console.info('Got platform/state message: %o', msg);
			if ( msg.data ) {
				var lockActive = msg.data.platformState === 'UserBlockingSystemTask';
				if ( lockActive ) {
					SolarNode.Platform.checkPlatformState(modal);
				} else if ( !SolarNode.Platform.willBeRestarting ){
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
