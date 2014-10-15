$(document).ready(function() {
	$('#associate-confirm-form').on('submit', function(event) {
		var pass = $('#invitation-certpass');
		var passAgain = $('#invitation-certpass-again');
		var reiterate = $('#invitation-certpass-reiterate');
		if ( pass.val().length > 0 && pass.val() !== passAgain.val() ) {
			event.preventDefault();
			alert(passAgain.data('mismatch'));
			return false;
		}
		
		if ( reiterate.hasClass('hidden') ) {
			event.preventDefault();
			reiterate.removeClass('hidden');
			return false;
		}
		
		return true;
	});
});
