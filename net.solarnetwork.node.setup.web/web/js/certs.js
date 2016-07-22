$(document).ready(function() {
	$('#btn-view-node-csr').click(function(event) {
		var a = this;
		event.preventDefault();
		$.getJSON(a.href, function(data) {
			$('#modal-csr-container').text(data.csr);
			$('#view-csr-modal').modal('show');
		});
	});
	$('#btn-renew-node-cert').click(function(event) {
		event.preventDefault();
		$('#renew-cert-modal').modal('show');
	});
	$('#renew-cert-modal').ajaxForm({
		dataType: 'json',
		beforeSubmit: function(formData, jqForm, options) {
			var ok = true,
				form = jqForm.get(0)
				p1 = $(form.elements['password']).val(),
				p2 = $(form.elements['passwordAgain']).val();
			if ( !(p1 && p1.length > 0 && p1 === p2) ) {
				$('#renew-cert-error-password-again').show();
				ok = false;
			}
			if ( ok ) {
				$('#renew-cert-error-password-again').hide();
			}
			return ok;
		},
		success: function(json, status, xhr, form) {
			var modal = $('#renew-cert-modal');
			if ( json && json.success === true ) {
				modal.find('.start').hide();
				modal.find('.success').show();
			} else {
				SolarNode.error(json.message, $('#renew-cert-modal .modal-body.start'));
			}
		},
		error: function(xhr, status, statusText) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, $('#renew-cert-modal .modal-body.start'));
		}
	}).on('shown', function() {
		$('#renew-cert-password').focus();
	});
	$('#btn-export-node-cert').click(function(event) {
		var a = this;
		event.preventDefault();
		$.getJSON(a.href, function(data) {
			$('#modal-cert-container').text(data.cert);
			$('#export-cert-modal').modal('show');
		});
	});
	$('#export-cert-modal').submit(function() {
		$('#export-cert-modal').modal('hide');
	});
	/*
	var importProgressBar = $('#import-cert-progress');
	$('#import-cert-modal').ajaxForm({
		dataType: 'json',
	    beforeSend: function() {
	        status.empty();
	        var percentVal = '0%';
	        importProgressBar.width(percentVal);
	    },
	    uploadProgress: function(event, position, total, percentComplete) {
	        var percentVal = percentComplete + '%';
	        importProgressBar.width(percentVal);
	    },
		success: function(json, status, xhr, form) {
			form.modal('hide');
		},
		error: function(xhr, status, statusText) {
			SolarNode.errorAlert(statusText);
		}
	});
	*/
});
