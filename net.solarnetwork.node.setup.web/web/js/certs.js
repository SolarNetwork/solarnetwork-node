$(document).ready(function() {
	$('#btn-view-node-csr').click(function(event) {
		var a = this;
		event.preventDefault();
		$.getJSON(a.href, function(data) {
			$('#modal-cert-container').text(data.csr);
			$('#view-cert-modal').modal('show');
		});
	});
});
