
$(document).ready(function pluginManagement() {
	'use strict';

	if ( !$('#plugins').length ) {
		return;
	}

	const pluginsSection = $('#plugins').first();
	const upgradeSection = $('#plugin-upgrades').first();
	const installedSection = $('#plugin-installed').first();

function compareVersions(v1, v2) {
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
}

function refreshPluginList(url) {
	SolarNode.showLoading($('#plugins-refresh'));
	$.getJSON(url, function(data) {
		SolarNode.hideLoading($('#plugins-refresh'));
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			// TODO: l10n
			SolarNode.warn('Error!', 'An error occured refreshing plugin information.', list);
			return;
		}
		populateUI();
	});
}

function versionLabel(plugin) {
	var version = plugin.version.major + '.' + plugin.version.minor;
	if ( plugin.version.micro > 0 ) {
		version += '.' + plugin.version.micro;
	}
	return version;
}

function groupNameForPlugin(plugin) {
	var match = (plugin.uid ? plugin.uid.match(/^net\.solarnetwork\.node\.(\w+)/) : null);
	if ( match == null ) {
		return plugin.uid;
	}
	var n = match[1];

	// special case: "io"
	if ( n === 'io' ) {
		return 'I/O';
	}

	// capitalize
	return (n.charAt(0).toUpperCase() + n.substring(1));
}

function pluginRefNameComparator(l, r) {
	if ( l.name < r.name ) {
		return -1;
	}
	if ( l.name > r.name ) {
		return 1;
	}
	return 0;
}

function groupPlugins(data) {
	var i, len;
	var plugin;
	var groupName;
	var installedPlugin;
	var result = {
		groupNames: [],		// String[]
		groups: {},   		// map of GroupName -> Plugin[]
		upToDate: [], 		// {uid, name}
		upgradable: [], 	// {uid, name, plugin}
		installed: {} 		// map of UID -> Plugin
	};

	for ( i = 0, len = data.installedPlugins.length; i < len; i++ ) {
		plugin = data.installedPlugins[i];
		result.installed[plugin.uid] = plugin;
		result.upToDate.push({uid:plugin.uid, name:plugin.info.name});
	}
	for ( i = 0, len = data.availablePlugins.length; i < len; i++ ) {
		plugin = data.availablePlugins[i];
		groupName = groupNameForPlugin(plugin);
		if ( result.groups[groupName] === undefined ) {
			result.groupNames.push(groupName);
			result.groups[groupName] = [];
		}
		result.groups[groupName].push(plugin);
		installedPlugin = result.installed[plugin.uid];
		if ( installedPlugin !== undefined && compareVersions(plugin.version, installedPlugin.version) > 0 ) {
			result.upgradable.push({uid:plugin.uid, name:plugin.info.name, plugin:plugin});
			result.upToDate.some(function(pluginRef, idx, array) {
				if ( plugin.uid === pluginRef.uid ) {
					array.splice(idx, 1);
					return true;
				}
			});
		}
	}
	result.groupNames.sort();
	result.upgradable.sort(pluginRefNameComparator);
	result.upToDate.sort(pluginRefNameComparator);
	return result;
}
	
function createGroup(groupName, container) {
	const group = $('<div class="accordion-item">');
	const heading = $('<h3 class="accordion-header">');
	const id = "Group-" +groupName.replace(/\W/g, '');
	const first = container.children().length === 0;
	const btn = $('<button type="button" class="accordion-button" data-bs-toggle="collapse" data-bs-target="#' +id+'">').text(groupName);
	if ( !first ) {
		btn.addClass('collapsed');
	}
	btn.appendTo(heading);
	heading.appendTo(group);
	var body = $('<div class="accordion-collapse collapse" data-bs-parent="#plugin-list">').attr('id', id);
	if ( first ) {
		body.addClass('show');
	}
	body.appendTo(group);
	var innerBody = $('<div class="accordion-body">');
	innerBody.appendTo(body);
	container.append(group);
	return innerBody;
}

function createPluginUI(plugin, installed, showVersions) {
	if ( !(plugin && plugin.uid) ) {
		return $(null);
	}
	var id = "Plugin-" +plugin.uid.replace(/\W/g, '-');
	var row = $('<div class="plugin row"/>').attr('id', id);
	var installedPlugin = installed[plugin.uid];
	var titleContainer = $('<div class="name col-sm-4"/>').text(plugin.info.name);
	titleContainer.appendTo(row);
	var descContainer = $('<div class="desc"/>').addClass(showVersions ? 'col-sm-5' : 'col-sm-6');
	if ( plugin.info.description ) {
		descContainer.text(plugin.info.description);
	}
	descContainer.appendTo(row);
	if ( showVersions === true ) {
		let versionContainer = $('<div class="version col-sm-1"/>').appendTo(row);
		let vLabel = $('<span class="badge text-bg-secondary">' +versionLabel(plugin) +'</span>');
		if ( compareVersions(plugin.version, installedPlugin.version) > 0 ) {
			// update available, add existing version to title
			$('<span class="badge text-bg-info suffix">' +versionLabel(installedPlugin) +'</span>').appendTo(titleContainer);
		}
		vLabel.appendTo(versionContainer);
	}
	var actionContainer = $('<div class="action col-sm-2 text-end"/>').appendTo(row);
	var button = undefined;
	var action = previewInstall;
	if ( installedPlugin === undefined ) {
		// not installed; offer to install it
		button = $('<button type="button" class="btn btn-sm btn-primary">').text(pluginsSection.data('msg-install'));
		actionContainer.append(button);
	} else if ( compareVersions(plugin.version, installedPlugin.version) > 0 ) {
		// update available
		button = $('<button type="button" class="btn btn-sm btn-info">').text(pluginsSection.data('msg-upgrade'));
		actionContainer.append(button);
	} else if ( plugin.coreFeature !== true ) {
		// installed, and not a core feature
		button = $('<button type="button" class="btn btn-sm btn-secondary"/>').text(pluginsSection.data('msg-remove'));
		actionContainer.append(button);
		action = previewRemove;
	} else if ( showVersions === true ) {
		// core, cannot remove
		$('<span class="badge text-bg-info"/>').text(installedSection.data('msg-unremovable')).appendTo(actionContainer);
	}
	if ( button !== undefined ) {
		button.on('click', function() { action(plugin); });
	}
	return row;
}
	
function populateUI() {
	var url = SolarNode.context.path('/a/plugins/list');
	var availableContainer = pluginsSection.children('.list-content');
	var upgradeContainer = upgradeSection.children('.list-content');
	var installedContainer = installedSection.children('.list-content');

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
		var k, kMax;
		var groupedPlugins = groupPlugins(data.data);
		var html = undefined;
		var groupBody = undefined;
		var groupName = undefined;
		var group = undefined;
		var plugin = undefined;

		// construct "upgradable" section
		html = $('<div id="plugin-upgrade-list"/>');
		for ( i = 0, iMax = groupedPlugins.upgradable.length; i < iMax; i++ ) {
			plugin = groupedPlugins.upgradable[i].plugin;
			html.append(createPluginUI(plugin, groupedPlugins.installed, true));
		}
		if ( iMax > 0 ) {
			upgradeContainer.html(html);
			upgradeSection.removeClass('hidden');
		} else {
			upgradeContainer.empty();
			upgradeSection.addClass('hidden');
		}

		// construct "up to date" section
		html = $('<div id="plugin-intalled-list"/>');
		for ( i = 0, iMax = groupedPlugins.upToDate.length; i < iMax; i++ ) {
			plugin = groupedPlugins.installed[groupedPlugins.upToDate[i].uid];
			html.append(createPluginUI(plugin, groupedPlugins.installed, true));
		}
		if ( iMax > 0 ) {
			installedContainer.html(html);
			installedSection.removeClass('hidden');
		} else {
			installedContainer.empty();
			installedSection.addClass('hidden');
		}

		// construct "available" section
		html = $('<div class="accordion" id="plugin-list"/>');
		for ( i = 0, iMax = groupedPlugins.groupNames.length; i < iMax; i++ ) {
			groupName = groupedPlugins.groupNames[i];
			groupBody = null; // don't create yet, we might filter out entire group
			group = groupedPlugins.groups[groupName];
			GROUP: for ( j = 0, jMax = group.length; j < jMax; j++ ) {
				plugin = group[j];

				// skip upgradable plugins
				for ( k = 0, kMax = groupedPlugins.upgradable.length; k < kMax; k++ ) {
					if ( plugin.uid === groupedPlugins.upgradable[k].uid ) {
						continue GROUP;
					}
				}

				// skip up-to-date plugins
				for ( k = 0, kMax = groupedPlugins.upToDate.length; k < kMax; k++ ) {
					if ( plugin.uid === groupedPlugins.upToDate[k].uid ) {
						continue GROUP;
					}
				}

				if ( groupBody === null ) {
					groupBody = createGroup(groupName, html);
				}
				groupBody.append(createPluginUI(plugin, groupedPlugins.installed));
			}
		}
		if ( html.children().length > 0 ) {
			availableContainer.html(html);
			pluginsSection.removeClass('hidden');
		} else {
			availableContainer.empty();
			pluginsSection.addClass('hidden');
		}
	});
}

function renderInstallPreview(data) {
	var form = $('#plugin-preview-install-modal');
	var container = $('#plugin-preview-install-list').empty();
	var installBtn = form.find('button[type=submit]');
	var i, len;
	var pluginToInstall;
	var list = $('<ul/>');
	var version;
	if ( data === undefined || data.success !== true || data.data === undefined ) {
		$('#plugin-preview-install-list').addClass('alert alert-error').html(
			data !== undefined && data.message ? '<p>' + data.message + '</p>' + (
			data.data ? '<p>' + data.data +'</p>' : ''
			) : 'Unknown error.');
		return;
	}
	for ( i = 0, len = data.data.pluginsToInstall.length; i < len; i++ ) {
		pluginToInstall = data.data.pluginsToInstall[i];
		version = versionLabel(pluginToInstall);
		$('<li/>').html('<b>' +pluginToInstall.info.name
				+'</b> <span class="label">' +version +'</span>').appendTo(list);
	}
	container.append(list);
	form.find('.restart-required').toggleClass('hidden', !data.data.restartRequired);
	installBtn.removeAttr('disabled');
}

function previewInstall(plugin) {
	var form = $('#plugin-preview-install-modal');
	var previewURL = form.attr('action') + '?uid=' +encodeURIComponent(plugin.uid);
	var container = $('#plugin-preview-install-list').empty().removeClass('alert').removeClass('alert-error');
	var title = form.find('h3');
	var installBtn = form.find('button[type=submit]');
	installBtn.attr('disabled', 'disabled');
	title.text(title.data('msg-install') +' ' +plugin.info.name);
	form.find('input[name=uid]').prop('disabled', false).val(plugin.uid);
	form.modal('show');
	$.getJSON(previewURL, renderInstallPreview);
}

function previewUpgradeAll(previewURL) {
	var form = $('#plugin-preview-install-modal');
	var container = $('#plugin-preview-install-list').empty();
	var title = form.find('h3');
	var installBtn = form.find('button[type=submit]');
	installBtn.attr('disabled', 'disabled');
	title.text(title.data('msg-install'));
	form.find('input[name=uid]').val('').prop('disabled', true);
	form.modal('show');
	$.getJSON(previewURL, renderInstallPreview);
}

function previewRemove(plugin) {
	var form = $('#plugin-preview-remove-modal');
	var container = $('#plugin-preview-remove-list').empty();
	var title = form.find('h3');
	title.text(title.data('msg-remove') +' ' +plugin.info.name);
	form.find('input[name=uid]').val(plugin.uid);
	form.find('.restart-required').toggleClass('hidden', false);
	form.modal('show');
	var list = $('<ul/>');
	var	version = versionLabel(plugin);
	$('<li/>').html('<b>' +plugin.info.name
			+'</b> <span class="label">' +version +'</span>').appendTo(list);
	container.append(list);
}

function handleInstall(form) {
	var progressBar = form.find('.progress');
	var progressFill = progressBar.find('.bar');
	var installBtn = form.find('button[type=submit]');
	var errorContainer = form.find('.message-container');
	var refreshPluginListOnModalClose = false;
	var keepPollingForStatus = true;

	var showAlert = function(msg) {
		SolarNode.hideLoading(installBtn);
		progressBar.addClass('hidden');
		SolarNode.error(SolarNode.i18n(installBtn.data('msg-error'), [msg]), errorContainer);
	};
	form.on('hidden.bs.modal', function() {
		// tidy up in case closed before completed
		SolarNode.hideLoading(installBtn);
		progressBar.addClass('hidden');
		installBtn.removeClass('hidden');

		// refresh the plugin list, if we've installed/removed anything
		if ( refreshPluginListOnModalClose === true ) {
			populateUI();
			refreshPluginListOnModalClose = false;
		}

		// in case we were still polling when close... don't bother to keep going
		keepPollingForStatus = false;

		// clear out any error/message
		errorContainer.addClass('hidden').empty();
	});
	form.ajaxForm({
		dataType: 'json',
		beforeSubmit: function(dataArray, form, options) {
			// start a progress bar on the install button so we know a install is happening
			progressBar.removeClass('hidden');
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
			var pollURL = SolarNode.context.path('/a/plugins/provisionStatus') +'?id='
					+encodeURIComponent(json.data.provisionID) +'&p=';
			var restartRequired = json.data.restartRequired;

			if ( restartRequired ) {
				form.find('.without-restart').addClass('hidden');
			}

			function handleRestart() {
				SolarNode.hideLoading(installBtn);
    				progressFill.css('width', '100%');
				form.find('.restarting').removeClass('hidden');
				form.find('.hide-while-restarting').addClass('hidden');
				setTimeout(function() {
					SolarNode.tryGotoURL(SolarNode.context.path('/a/home'));
				}, 10000);
			}

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
				    			if ( handleRestart ) {
				    				// lets assume we're restarting
				    				handleRestart();
				    			} else {
				    				showAlert(xhr.statusText);
				    			}
				    		} else if ( !(progress < 100) ) {
							SolarNode.hideLoading(installBtn);
				    			installBtn.addClass('hidden');
				    			if ( restartRequired ) {
				    				handleRestart();
				    			} else {
					    			SolarNode.info(SolarNode.i18n(installBtn.data('msg-success')), errorContainer);
					    			errorContainer.removeClass('hidden');
				    				progressBar.addClass('hidden');
					    			refreshPluginListOnModalClose = true;
				    			}
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
}

	if ( pluginsSection.length === 1 && upgradeSection.length === 1 ) {
		populateUI();
	};
	$('#plugins-refresh').on('click', function(event) {
		event.preventDefault();
		refreshPluginList(this.dataset.action);
	});
	$('#plugins-upgrade-all').on('click', function(event) {
		event.preventDefault();
		previewUpgradeAll(this.dataset.action);
	});
	$('#plugin-preview-install-modal').first().each(function() {
		handleInstall($(this));
	});
	$('#plugin-preview-remove-modal').first().each(function() {
		handleInstall($(this));
	});
});