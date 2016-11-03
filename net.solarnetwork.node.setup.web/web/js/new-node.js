(function() {
'use strict';

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
	
	$('#associate-import-backup-form').on('submit', function(event) {
		var form = $(event.target),
			submitBtn = form.find('button[type=submit]');
		submitBtn.attr('disabled', 'disabled');
		SolarNode.showSpinner(submitBtn);
	});

	$('#associate-restore-list-container').on('click', 'div.menu-item', function(event) {
		var row = $(this), 
			selectedCount = 0,
			submit = $('#associate-restore-backup-form button[type=submit]');
		row.toggleClass('selected');
		selectedCount = row.parent().children('.selected').size();
		if ( selectedCount < 1 ) {
			submit.attr('disabled', 'disabled');
		} else {
			submit.removeAttr('disabled');
		}
	});

	$('#associate-restore-backup-form').ajaxForm({
		dataType : 'json',
		beforeSubmit : function(dataArray, form, options) {
			var providers = SolarNode.Backups.selectedProviders($('#associate-restore-list-container')),
				submitBtn = form.find('button[type=submit]');
			Array.prototype.splice.apply(dataArray, [dataArray.length, 0].concat(providers));
			submitBtn.attr('disabled', 'disabled');
			SolarNode.showSpinner(submitBtn);
		},
		success : function(json, status, xhr, form) {
			if ( json.success !== true ) {
				SolarNode.error(json.message, $('#associate-restore-backup-form'));
				return;
			}
			var form = $('#associate-restore-backup-form');
			SolarNode.info(json.message, $('#associate-restore-list-container').empty());
			form.find('button, p').remove();
			form.find('.progress.hide').removeClass('hide');
			setTimeout(function() {
				SolarNode.Backups.handleRestart(SolarNode.context.path('/a/settings'));
			}, 10000);
		},
		error : function(xhr, status, statusText) {
			SolarNode.error("Error restoring backup: " +statusText, $('#associate-restore-backup-form'));
		},
		complete : function() {
			var submitBtn = $('#associate-restore-backup-form button[type=submit]');
			submitBtn.removeAttr('disabled');
			SolarNode.hideSpinner(submitBtn);
		}
	}).each(function(idx, el) {
		$.getJSON(SolarNode.context.path('/associate/importedBackup'), function(json) {
			if ( json.success !== true ) {
				SolarNode.error(json.message, $('#associate-restore-backup-form'));
				return;
			}
			SolarNode.Backups.generateBackupList(json.data, $('#associate-restore-list-container'));
		});
	});
});

}());
