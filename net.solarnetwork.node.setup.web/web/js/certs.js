$(document).ready(function() {
	$('#btn-view-node-csr').click(function(event) {
		var a = this;
		event.preventDefault();
		$.getJSON(a.href, function(data) {
			$('#modal-csr-container').text(data.csr);
			$('#view-csr-modal').modal('show');
		});
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
