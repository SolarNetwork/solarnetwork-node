$(document).ready(function() {
	var newUsername;
	
	$('#login-username').select().focus();

	$('#change-password-form').ajaxForm({
		dataType: 'json',
		beforeSubmit: function(formData, jqForm, options) {
			$('#change-password-success').addClass('hidden');
			return true;
		},
		success: function(json, status, xhr, jqForm) {
			var form;
			if ( json && json.success ) {
				form = jqForm.get(0);
				$('#change-password-success').removeClass('hidden');
				$(form.elements['oldPassword']).attr('value', '');
				form.reset();
			} else {
				SolarNode.error(json.message);
			}
		},
		error: function(xhr, status, statusText) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message);
		}
	});
	
	$('#change-username-form').ajaxForm({
		dataType: 'json',
		beforeSubmit: function(formData, jqForm, options) {
			$('#change-username-success').addClass('hidden');
			return true;
		},
		success: function(json, status, xhr, jqForm) {
			var form;
			if ( json && json.success ) {
				form = jqForm.get(0);
				$('#change-username-success').removeClass('hidden');
				$('.active-user-display').text(form.elements['username'].value);
			} else {
				SolarNode.error(json.message);
			}
		},
		error: function(xhr, status, statusText) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message);
		}
	});
	
	// focus on the change password form's old password, unless that has a pre-filled value
	var oldPasswordField = $('#old-password');
	if ( oldPasswordField.val() ) {
		$('#login-password').focus();
	} else {
		oldPasswordField.focus();
	}
	
});
