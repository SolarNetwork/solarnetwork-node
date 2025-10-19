(function() {
'use strict';

SolarNode.Backups = {};

SolarNode.Backups.generateBackupList = function(backupInfo, container) {
	container.empty();
	if ( !backupInfo || !Array.isArray(backupInfo.resourceInfos) ) {
		return;
	}
	
	function providerInfo(providerKey) {
		return backupInfo.providerInfos.find(function(info) {
			return (info.providerKey === providerKey);
		});
	}
	
	function resourcesForProvider(providerKey) {
		return backupInfo.resourceInfos.filter(function(info) {
			return (info.providerKey === providerKey);
		});
	}

	// sort providers by display name
	backupInfo.providerInfos.sort(function(l,r) {
		return (l.name ? l.name : l.providerKey).localeCompare(r.name ? r.name : r.providerKey);
	});

	backupInfo.providerInfos.forEach(function(provider) {
		var row = $('<div class="row menu-item py-2" data-provider="'+provider.providerKey+'"></div>'),
			resources = resourcesForProvider(provider.providerKey);
		if (provider.defaultShouldRestore !== false) {
			row.addClass('selected');
		}
		row.append('<div class="col-sm-3 col-md-2 text-sm-center"><i class="bi bi-check-circle fs-3 checkmark" aria-hidden="true"></i></div>');
		row.append($('<div class="col-sm-6 col-md-7"></div>')
				.append($('<h4>').text(provider.name))
				.append($('<p>').text(provider.description)));
		row.append($('<div class="col-3">').append($('<h4>').append($('<small>').text(resources.length + ' ' 
				+ container.data(resources.length == 1 ? 'msg-item' : 'msg-items')))));
		container.append(row);
	});
};

SolarNode.Backups.selectedProviders = function(container) {
	return container.find('.menu-item.selected').map(function(idx, item) {
		return { name : 'providers', value : $(item).data('provider') };
	}).toArray();
};

}());

$(document).ready(function csvBackupManagement() {
	'use strict';

	if ( !$('#csv-backups').length ) {
		return;
	}
	
	/**
	 * CSV Backup key entity.
	 * 
	 * @typedef {Object} CsvBackupKey
	 * @property {string} timestamp the timestamp
	 * @property {string} key the key
	 */

	/** @type HTMLFormElement */
	const csvExportForm = document.getElementById('settings-csv-io-export');

	/** @type HTMLFormElement */
	const csvImportForm = document.getElementById('settings-csv-io-import');
	
	const listCsvBackupsUrl = csvImportForm.action.replace(/\/[^/]+$/, '/list-backups?serviceId=');
	
	$('#settings-csv-io-service').on('change', function csvServiceChange() {
		const serviceId = $(this).val();
		csvExportForm.elements['serviceId'].value = serviceId;
		csvImportForm.elements['serviceId'].value = serviceId;
		
		// populate auto-backups
		
		/** @type HTMLSelectElement */
		const csvExportSelect = csvExportForm.elements['key'];
		while (csvExportSelect.length > 1) {
			csvExportSelect.remove(1);
		}
		return $.getJSON(listCsvBackupsUrl + encodeURIComponent(serviceId), (data) => {
			if ( data && data.success === true ) {
				/** @type CsvBackupKey[] */
				const backupList = data.data;
				backupList.reverse();
				for ( let backup of  backupList ) {
					csvExportSelect.add(new Option(moment(backup.timestamp).format('YYYY-MM-DD HH:mm'), backup.key));
				}
			}
		});
	}).trigger('change');
});
