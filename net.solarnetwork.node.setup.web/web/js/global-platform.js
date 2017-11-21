'use strict';
SolarNode.Platform = (function() {
	var self = {};
	
	function getActivePlatformTaskInfo(callback) {
		$.getJSON(SolarNode.context.path('/platform/task'), callback);
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
				getActivePlatformTaskInfo(function(data) {
					
					setPlatformLockModalVisible(modal, true);
				});
			} else {
				setPlatformLockModalVisible(modal, false);
			}
		});
	}

	function setPlatformLockModalVisible(modal, visible) {
		modal.modal(visible ? 'show' : 'hide');
	}
	
	return Object.defineProperties(self, {
		checkPlatformState : { value : checkPlatformState },
		setPlatformLockModalVisible : { value : setPlatformLockModalVisible },
	});
}());

$(document).ready(function() {
	$('#platform-lock-modal').first().each(function() {
		var modal = $(this);
		
		function handlePlatformStateMessage(msg) {
			var msg = JSON.parse(msg.body);
			console.info('Got platform/state message: %o', msg);
			if ( msg.data ) {
				setPlatformLockModalVisible(msg.data.platformState === 'UserBlockingSystemTask')
				SolarNode.Platform.handlePlatformStateChangeEvent(modal, msg.data);
			}
		};

		// subscribe for state change messages
		SolarNode.WebSocket.subscribeToTopic('/pub/topic/platform/state', handlePlatformStateMessage);
		
		// check the current state
		SolarNode.Platform.checkPlatformState(modal);
	});
});
