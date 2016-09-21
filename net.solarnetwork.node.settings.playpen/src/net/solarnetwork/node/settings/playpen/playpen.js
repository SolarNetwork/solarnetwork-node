$(document).ready(function() {
	'use strict';
	
	$('button.playpen-setting-custom-button').on('click', function(event) {
		var btn = $(event.target),
			container = btn.parents('.setup-resource-container');
		alert("Why, hello there, setting " +container.data('setting-id') +"!");
	});

});
