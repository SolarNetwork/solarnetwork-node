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
		var row = $('<div class="row menu-item selected py-2" data-provider="'+provider.providerKey+'"></div>'),
			resources = resourcesForProvider(provider.providerKey);
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