$(document).ready(function() {
	$('#associate-confirm-form').on('submit', function(event) {
		var pass = $('#invitation-certpass');
		var passAgain = $('#invitation-certpass-again');
		var reiterate = $('#invitation-certpass-reiterate');
		
		function fail(msg) {
			event.preventDefault();
			if ( msg ) {
				alert(msg);
			}
			return false;
		}
		
		if ( pass.val().length > 0 ) {
			if ( pass.val() !== passAgain.val() ) {
				return fail(passAgain.data('mismatch'));
			} else if ( pass.val().length < 8 ) {
				return fail(passAgain.data('tooshort'));
			}
		}
		
		if ( reiterate.hasClass('hidden') ) {
			event.preventDefault();
			reiterate.removeClass('hidden');
			return false;
		}
		
		return true;
	});
});
