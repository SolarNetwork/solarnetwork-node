$(document).ready(function securityTokenManagement() {
	'use strict';

	if ( !$('#sectoks').length ) {
		return;
	}

	function toggleBeforeAfter(container, before) {
		container.find('.before').toggleClass('hidden', !before);
		container.find('.after').toggleClass('hidden', before);
	}
	
	// create token form
	const createSecurityTokenModal = $('#create-security-token-modal');
	
	createSecurityTokenModal.ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			createSecurityTokenModal.find('button[type=submit]').prop('disabled', true);
			return true;
		},
		success: function(json) {
			if ( json && json.success === true && json.data ) {
				createSecurityTokenModal[0].elements['tokenId'].value = json.data.key;
				createSecurityTokenModal[0].elements['tokenSecret'].value = json.data.value;
				toggleBeforeAfter(createSecurityTokenModal, false);
			} else {
				SolarNode.error(json.message, createSecurityTokenModal.find('.modal-body.start'));
			}
		},
		error: function(xhr) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, createSecurityTokenModal.find('.modal-body.start'));
		}
	})
	.on('shown', function() {
		createSecurityTokenModal.find('input[name=name]').focus();
	})
	.on('hidden', function() {
		if ( createSecurityTokenModal[0].elements['tokenSecret'].value ) {
			// created new token; refresh page
			window.location.reload();
			return;
		}
		toggleBeforeAfter(createSecurityTokenModal, true);
		this.reset();
		createSecurityTokenModal.find('button[type=submit]').prop('disabled', false);
	});
	
	$('#create-token-download-csv').on('click', function downloadTokenCsv() {
		const link = this;
		const form = createSecurityTokenModal[0];
		const csv = 'Token ID,Token Secret,Name,Description\r\n'
			+ form.elements.tokenId.value
			+ ','
			+ form.elements.tokenSecret.value
			+ ',"'
			+ form.elements.name.value.replaceAll('"','""')
			+ '","'
			+ form.elements.description.value.replaceAll('"','""')
			+ '"\r\n';
		const uri = encodeURI('data:text/csv;charset=utf-8,'+csv);
		link.setAttribute("href", uri);
	});
	
	// edit token form

	const editSecurityTokenModal = $('#edit-security-token-modal');
		
	$('#sectoks').on('click', function handleSecurityTokenEditLink(event) {
		const btn = $(event.target);
		if ( !btn.hasClass('edit-link') ) {
			return;
		}
		const tokenId = btn.data('tokenId');
		if ( !tokenId ) {
			return;
		}
		editSecurityTokenModal[0].elements.id.value = tokenId;
		editSecurityTokenModal[0].elements.name.value = btn.data('tokenName');
		editSecurityTokenModal[0].elements.description.value = btn.data('tokenDescription');
		editSecurityTokenModal.modal('show');
	});
	
	editSecurityTokenModal.find('button[name=delete]').on('click', function handleSecurityTokenDelete() {
		const form = editSecurityTokenModal[0];
		$.ajax({
			type: 'DELETE',
			dataType: 'json',
			url: form.action + '?tokenId=' +encodeURIComponent(form.elements.id.value),
			beforeSend: (xhr) => SolarNode.csrf(xhr),
		}).then(() => {
			window.location.reload();
		}, (xhr, status) => {
			console.log("Failed to delete token [%s]: %s", form.elements.id.value, status);
		});
	});
	
	editSecurityTokenModal.ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			createSecurityTokenModal.find('button[type=submit],button[name=delete]').prop('disabled', true);
			
			return true;
		},
		success: function(json) {
			if ( json && json.success === true ) {
				window.location.reload();
			} else {
				SolarNode.error(json.message, editSecurityTokenModal.find('.modal-body.start'));
			}
		},
		error: function(xhr) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, editSecurityTokenModal.find('.modal-body.start'));
		}
	})
	.on('show', function(event) {
		var target = event.target;
		console.log("target = %o", target);
	})
	.on('shown', function() {
		editSecurityTokenModal.find('input[name=name]').focus();
	})
	.on('hidden', function() {
		this.reset();
		editSecurityTokenModal.find('button[type=submit],button[name=delete]').prop('disabled', false);
	});

});