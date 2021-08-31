$(document).ready(function() {
	'use strict';
	
	$('img.camera-ffmpeg').each(function() {
		var img = $(this),
			imgId = img.closest('.setup-resource-container').data('mediaResourceId');
		if ( imgId ) {
			img.attr('src', '../rsrc/'+imgId);
		}
	});

});
