$(document).ready(function() {
	'use strict';
	
	$('img.camera-motion').each(function() {
		var img = $(this),
			imgId = img.closest('.setup-resource-container').data('snapId');
		img.attr('src', '../rsrc/'+imgId);
	});

});
