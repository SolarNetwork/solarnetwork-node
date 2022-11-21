$(document).ready(function() {
	'use strict';

	function setupPlaypen(container) {
		container.find('button.playpen-setting-custom-button').on('click', function(event) {
			var btn = $(event.target),
				foo = btn.parent().data('foo');
			alert("Why, hello there! foo is " +foo +"!");
		});
	}
	
	$('body').on('sn.settings.component.loaded', function(event, container) {
		setupPlaypen(container);
	});	

	setupPlaypen($());
});
