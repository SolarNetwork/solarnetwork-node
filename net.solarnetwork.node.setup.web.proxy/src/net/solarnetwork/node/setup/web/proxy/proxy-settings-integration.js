$(document).ready(function() {
	'use strict';
	
	$('button.web-proxy-open').on('click', function(event) {
		var btn = $(event.target),
			configID = btn.parent().data('config-id');
		if ( configID ) {
			window.location.assign(SolarNode.context.path(`/a/webproxy/${configID}/`));
		}
	});

});
