$(document).ready(function() {
	'use strict';
	
	function setupProxyUi(container) {
		container.find('a.web-proxy-open').on('click', function(event) {
			var btn = $(event.target),
				configID = btn.parent().data('config-id');
			btn.attr('href', configID 
				? SolarNode.context.path(`/a/webproxy/${configID}/`)
				: '#');
		});		
	}

	$('body').on('sn.settings.component.loaded', function(_event, container) {
		setupProxyUi(container);
	});	

	setupProxyUi($());
});
