$(document).ready(function() {
	$('#login-username').select().focus();

	$('#change-password-form').ajaxForm({
		dataType: 'json',
		beforeSubmit: function(formData, jqForm, options) {
			$('#change-password-success').addClass('hidden');
			return true;
		},
		success: function(json, status, xhr, form) {
			if ( json && json.success ) {
				$('#change-password-success').removeClass('hidden');
				$('#change-password-form').get(0).reset();
			} else {
				SolarNode.error(json.message);
			}
		},
		error: function(xhr, status, statusText) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message);
		}
	})
	$('#old-password').focus();
});
