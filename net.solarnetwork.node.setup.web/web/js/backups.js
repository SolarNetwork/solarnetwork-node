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

	backupInfo.providerInfos.forEach(function(provider) {
		var row = $('<div class="row-fluid menu-item selected" data-provider="'+provider.providerKey+'"></div>'),
			resources = resourcesForProvider(provider.providerKey);
		row.append('<div class="span1"><i class="fa fa-check fa-lg checkmark" aria-hidden="true"></i></div>');
		row.append($('<div class="span9"></div>')
				.append($('<h4>').text(provider.name))
				.append($('<p>').text(provider.description)));
		row.append($('<div class="span2">').append($('<h4>').append($('<small>').text(resources.length + ' ' 
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