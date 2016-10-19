$(document).ready(function() {
	'use strict';
	
	$('button.playpen-setting-custom-button').on('click', function(event) {
		var btn = $(event.target),
			foo = btn.parent().data('foo');
		alert("Why, hello there! foo is " +foo +"!");
	});

});
