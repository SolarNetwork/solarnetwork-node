SolarNode.Plugins = {
		
};

SolarNode.Plugins.runtime = {};

SolarNode.Plugins.compareVersions = function(v1, v2) {
	var result = v1.major - v2.major;
	if ( result !== 0 ) {
		return result;
	}
	result = v1.minor - v2.minor;
	if ( result !== 0 ) {
		return result;
	}
	result = v1.micro - v2.micro;
	if ( result !== 0 ) {
		return result;
	}
	// ignoring qualifiers
	return 0;
};

SolarNode.Plugins.refreshPluginList = function(url, container, upgradeContainer, installedContainer) {
	SolarNode.showLoading($('#plugins-refresh'));
	$.getJSON(url, function(data) {
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			SolarNode.hideLoading($('#plugins-refresh'));
			// TODO: l10n
			SolarNode.warn('Error!', 'An error occured refreshing plugin information.', list);
			return;
		}
		SolarNode.Plugins.populateUI(container, upgradeContainer, installedContainer);
	});
};

SolarNode.Plugins.versionLabel = function(plugin) {
	var version = plugin.version.major + '.' + plugin.version.minor;
	if ( plugin.version.micro > 0 ) {
		version += '.' + plugin.version.micro;
	}
	return version;
};

SolarNode.Plugins.populateUI = function(availableSection, upgradeSection, installedSection) {
	var url = SolarNode.context.path('/plugins/list');
	var availableContainer = availableSection.children('.list-content');
	var upgradeContainer = upgradeSection.children('.list-content');
	var installedContainer = installedSection.children('.list-content');
	
	var groupNameForPlugin = function(plugin) {
		var match = plugin.uid.match(/^net\.solarnetwork\.node\.(\w+)/);
		if ( match == null ) {
			return plugin.uid;
		}
		var n = match[1];
		
		// some special cases here...
		// TODO: l10n
		if ( n === 'io' ) {
			return "Communication Support";
		}
		if ( n === 'hw' ) {
			return "Hardware Support";
		}
		
		// default: capitalize
		
		return (n.charAt(0).toUpperCase() + n.substring(1));
	};
	
	var pluginRefNameComparator = function(l, r) {
		if ( l.name < r.name ) {
			return -1;
		}
		if ( l.name > r.name ) {
			return 1;
		}
		return 0;
	};
	
	var groupPlugins = function(data) {
		var i, len;
		var plugin;
		var groupName;
		var installedPlugin;
		var result = {
			groupNames: [],		// String[]
			groups: {},   		// map of GroupName -> Plugin[]
			upToDate: [], 		// {uid, name}
			installed: {}, 		// map of UID -> Plugin
			upgradableNames: [],// String[]
			upgradable: {} 		// map of UID -> Plugin
		};
		for ( i = 0, len = data.installedPlugins.length; i < len; i++ ) {
			plugin = data.installedPlugins[i];
			result.installed[plugin.uid] = plugin;
			// the following assumes plugin names are unique, which seems OK for now
			result.upToDate.push({uid:plugin.uid, name:plugin.info.name});
		}
		for ( i = 0, len = data.availablePlugins.length; i < len; i++ ) {
			plugin = data.availablePlugins[i];
			groupName = groupNameForPlugin(plugin);
			if ( result.groups[groupName] === undefined ) {
				result.groupNames.push(groupName);
				// note we assume plugins already sorted by name
				result.groups[groupName] = [];
			}
			result.groups[groupName].push(plugin);
			installedPlugin = result.installed[plugin.uid];
			// the following assumes plugin names are unique, which seems OK for now
			if ( installedPlugin !== undefined && SolarNode.Plugins.compareVersions(plugin.version, installedPlugin.version) > 0 ) {
				result.upgradableNames.push(plugin.info.name);
				result.upgradable[plugin.info.name] = plugin;
				
				// check if this is upgradable, to remove from "installed" section
				result.upToDate.some(function(pluginRef, idx, array) {
					if ( plugin.uid === pluginRef.uid ) {
						array.splice(idx, 1);
						return true;
					}
				});
			}
		}
		result.groupNames.sort();
		result.upgradableNames.sort();
		result.upToDate.sort(pluginRefNameComparator);
		return result;
	};
	
	var createGroup = function(groupName, container) {
		var group = $('<div class="accordion-group"/>');
		var heading = $('<div class="accordion-heading"/>');
		var id = "Group-" +groupName.replace(/\W/g, '');
		$('<a class="accordion-toggle" data-toggle="collapse" data-parent="#plugin-list"/>').attr('href', '#'+id).text(groupName).appendTo(heading);
		heading.appendTo(group);
		var body = $('<div class="accordion-body collapse"/>').attr('id', id);
		if ( container.children().length === 0 ) {
			body.addClass('in');
		}
		body.appendTo(group);
		var innerBody = $('<div class="accordion-inner"/>');
		innerBody.appendTo(body);
		container.append(group);
		return innerBody;
	};
	
	var createPluginUI = function(plugin, installed, showVersions) {
		var id = "Plugin-" +plugin.uid.replace(/\W/g, '-');
		var row = $('<div class="plugin row-fluid"/>').attr('id', id);
		var installedPlugin = installed[plugin.uid];
		var titleContainer = $('<div/>').addClass(showVersions ? 'span9' : 'span10').text(plugin.info.name);
		titleContainer.appendTo(row);
		if ( showVersions === true ) {
			var versionContainer = $('<div class="span1"/>').appendTo(row);
			var versionLabel = $('<span class="label">' +SolarNode.Plugins.versionLabel(plugin) +'</span>');
			if ( installedPlugin === undefined ) {
				// not installed... leave default style
			} else if ( SolarNode.Plugins.compareVersions(plugin.version, installedPlugin.version) > 0 ) {
				// update available
				//versionLabel.addClass('label-info');
				
				// also add existing version to title
				$('<span class="label suffix">' +SolarNode.Plugins.versionLabel(installedPlugin) +'</span>').appendTo(titleContainer);
			} else {
				// installed
				//versionLabel.addClass('label-important');
			}
			versionLabel.appendTo(versionContainer);
		}
		var actionContainer = $('<div class="span2"/>').appendTo(row);
		var button = undefined;
		var action = SolarNode.Plugins.previewInstall;
		if ( installedPlugin === undefined ) {
			// not installed; offer to install it
			button = $('<button type="button" class="btn btn-small btn-primary">').text(availableSection.data('msg-install'));
			actionContainer.append(button);
		} else if ( SolarNode.Plugins.compareVersions(plugin.version, installedPlugin.version) > 0 ) {
			// update available
			button = $('<button type="button" class="btn btn-small btn-info">').text(availableSection.data('msg-upgrade'));
			actionContainer.append(button);
		} else {
			// installed
			button = $('<button type="button" class="btn btn-small btn-danger"/>').text(availableSection.data('msg-remove'));
			actionContainer.append(button);
			action = SolarNode.Plugins.previewRemove;
		}
		button.click(function() {
			action(plugin);
		});
		return row;
	};
	
	SolarNode.showLoading($('#plugins-refresh'));
	$.getJSON(url, function(data) {
		SolarNode.hideLoading($('#plugins-refresh'));
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			availableContainer.empty();
			upgradeContainer.empty();
			// TODO: l10n
			SolarNode.warn('Error!', 'An error occured loading plugin information.', availableContainer);
			return;
		}
		var i, iMax;
		var j, jMax;
		var groupedPlugins = groupPlugins(data.data);
		var html = undefined;
		var groupBody = undefined;
		var groupName = undefined;
		var group = undefined;
		var plugin = undefined;
		
		// construct "upgradable" section
		html = $('<div id="plugin-upgrade-list"/>');
		for ( i = 0, iMax = groupedPlugins.upgradableNames.length; i < iMax; i++ ) {
			plugin = groupedPlugins.upgradable[groupedPlugins.upgradableNames[i]];
			html.append(createPluginUI(plugin, groupedPlugins.installed, true));
		}
		if ( iMax > 0 ) {
			upgradeContainer.html(html);
			upgradeSection.removeClass('hide');
		} else {
			upgradeSection.addClass('hide');
		}
		
		// construct "up to date" section
		html = $('<div id="plugin-intalled-list"/>');
		for ( i = 0, iMax = groupedPlugins.upToDate.length; i < iMax; i++ ) {
			plugin = groupedPlugins.installed[groupedPlugins.upToDate[i].uid];
			html.append(createPluginUI(plugin, groupedPlugins.installed, true));
		}
		if ( iMax > 0 ) {
			installedContainer.html(html);
			installedSection.removeClass('hide');
		} else {
			installedSection.addClass('hide');
		}
		
		// construct "available" section
		html = $('<div class="accordion" id="plugin-list"/>');
		for ( i = 0, iMax = groupedPlugins.groupNames.length; i < iMax; i++ ) {
			groupName = groupedPlugins.groupNames[i];
			groupBody = createGroup(groupName, html);
			group = groupedPlugins.groups[groupName];
			for ( j = 0, jMax = group.length; j < jMax; j++ ) {
				plugin = group[j];
				groupBody.append(createPluginUI(plugin, groupedPlugins.installed));
			}
		}
		availableContainer.html(html);
	});
};

SolarNode.Plugins.previewInstall = function(plugin) {
	var form = $('#plugin-preview-install-modal');
	var previewURL = form.attr('action') + '?uid=' +encodeURIComponent(plugin.uid);
	var container = $('#plugin-preview-install-list').empty();
	var title = form.find('h3');
	var installBtn = form.find('button[type=submit]');
	installBtn.attr('disabled', 'disabled');
	title.text(title.data('msg-install') +' ' +plugin.info.name);
	form.find('input[name=uid]').val(plugin.uid);
	form.modal('show');
	$.getJSON(previewURL, function(data) {
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			// TODO: l10n
			SolarNode.warn('Error!', 'An error occured loading plugin information.', list);
			return;
		}
		var i, len;
		var pluginToInstall;
		var list = $('<ul/>');
		var version;
		for ( i = 0, len = data.data.pluginsToInstall.length; i < len; i++ ) {
			pluginToInstall = data.data.pluginsToInstall[i];
			version = SolarNode.Plugins.versionLabel(pluginToInstall);
			$('<li/>').html('<b>' +pluginToInstall.info.name  
					+'</b> <span class="label">' +version +'</span>').appendTo(list);
		}
		container.append(list);
		installBtn.removeAttr('disabled');
	});
};

SolarNode.Plugins.previewRemove = function(plugin) {
	var form = $('#plugin-preview-remove-modal');
	var container = $('#plugin-preview-remove-list').empty();
	var title = form.find('h3');
	title.text(title.data('msg-remove') +' ' +plugin.info.name);
	form.find('input[name=uid]').val(plugin.uid);
	form.modal('show');
	var list = $('<ul/>');
	var	version = SolarNode.Plugins.versionLabel(plugin);
	$('<li/>').html('<b>' +plugin.info.name  
			+'</b> <span class="label">' +version +'</span>').appendTo(list);
	container.append(list);
};

SolarNode.Plugins.handleInstall = function(form) {
	var progressBar = form.find('.progress');
	var progressFill = progressBar.find('.bar');
	var installBtn = form.find('button[type=submit]');
	var errorContainer = form.find('.message-container');
	var refreshPluginListOnModalClose = false;
	var keepPollingForStatus = true;
	
	var showAlert = function(msg) {
		SolarNode.hideLoading(installBtn);
		progressBar.addClass('hide');
		SolarNode.error(SolarNode.i18n(installBtn.data('msg-error'), [msg]), errorContainer);
	};
	form.on('hidden', function() {
		// tidy up in case closed before completed
		SolarNode.hideLoading(installBtn);
		progressBar.addClass('hide');
		installBtn.removeClass('hide');
		
		// refresh the plugin list, if we've installed/removed anything
		if ( refreshPluginListOnModalClose === true ) {
			SolarNode.Plugins.populateUI($('#plugins'), $('#plugin-upgrades'), $('#plugin-installed'));
			refreshPluginListOnModalClose = false;
		}
		
		// in case we were still polling when close... don't bother to keep going
		keepPollingForStatus = false;
		
		// clear out any error/message
		errorContainer.addClass('hide').empty();
	});
	form.ajaxForm({
		dataType: 'json',
		beforeSubmit: function(dataArray, form, options) {
			// start a progress bar on the install button so we know a install is happening
			progressBar.removeClass('hide');
			progressFill.css('width', '0%');
			keepPollingForStatus = true;
			errorContainer.empty();
			SolarNode.showLoading(installBtn);
		},
		success: function(json, status, xhr, form) {
			if ( json.success !== true ) {
				SolarNode.hideLoading(installBtn);
				showAlert(json.message);
				return;
			}
			// TODO: support message? var message = json.data.statusMessage;
			var progress = Math.round(json.data.overallProgress * 100);
			var pollURL = SolarNode.context.path('/plugins/provisionStatus') +'?id=' 
					+encodeURIComponent(json.data.provisionID) +'&p=';
			(function poll() {
			    $.ajax({ 
			    	url: (pollURL + progress),
			    	dataType: "json",
			    	success: function(json) {
			    		if ( json.success === true && json.data !== undefined ) {
			    			progress = Math.round(json.data.overallProgress * 100);
			    			progressFill.css('width', progress +'%');
			    		} else {
			    			if ( json.message !== undefined ) {
			    				showAlert(json.message);
			    			}
			    			keepPollingForStatus = false;
			    		}
			    	}, 
			    	complete: function(xhr, status) {
			    		if ( status === 'error' ) {
			    			showAlert(xhr.statusText);
			    		} else if ( !(progress < 100) ) {
							SolarNode.hideLoading(installBtn);
			    			SolarNode.info(SolarNode.i18n(installBtn.data('msg-success')), errorContainer);
			    			progressBar.addClass('hide');
			    			installBtn.addClass('hide');
			    			errorContainer.removeClass('hide');
			    			refreshPluginListOnModalClose = true;
			    		} else if ( keepPollingForStatus ) {
			    			poll();
			    		}
			    	}, 
			    	timeout: 20000,
			    });
			})();
		},
		error: function(xhr, status, statusText) {
			SolarNode.hideLoading(installBtn);
			showAlert(statusText);
		}
	});
};

$(document).ready(function() {
	var pluginsSection = $('#plugins').first();
	var upgradeSection = $('#plugin-upgrades').first();
	var installedSection = $('#plugin-installed').first();
	if ( pluginsSection.size() === 1 && upgradeSection.size() === 1 ) {
		SolarNode.Plugins.populateUI(pluginsSection, upgradeSection, installedSection);
	};
	$('#plugins-refresh').click(function(event) {
		event.preventDefault();
		SolarNode.Plugins.refreshPluginList($(this).attr('href'), pluginsSection, upgradeSection, installedSection);
	});
	$('#plugin-preview-install-modal').first().each(function() {
		SolarNode.Plugins.handleInstall($(this));
	});
	$('#plugin-preview-remove-modal').first().each(function() {
		SolarNode.Plugins.handleInstall($(this));
	});
});
