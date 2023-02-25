$(document).ready(function() {
	'use strict';
	
	function setupCameraImage(container) {
		container.find('img.camera-ffmpeg').each(function() {
			var img = $(this),
				imgId = img.closest('.setup-resource-container').data('mediaResourceId');
			if ( imgId ) {
				img.attr('src', '../rsrc/'+imgId);
			}
		});
	}
	
	$('body').on('sn.settings.component.loaded', function(event, container) {
		setupCameraImage(container);
	});	

	setupCameraImage($());
});
