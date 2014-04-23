SolarNode.Plugins = {
		
};

SolarNode.Plugins.runtime = {};

SolarNode.Plugins.populateUI = function(container) {
	var url = SolarNode.context.path('/plugins');
	
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
	
	var groupPlugins = function(data) {
		var i, len;
		var plugin;
		var groupName;
		var result = {
			groupNames: [],	// String[]
			groups: {},   	// map of GroupName -> Plugin[]
			installed: {} 	// map of UID -> Plugin
		};
		for ( i = 0, len = data.availablePlugins.length; i < len; i++ ) {
			plugin = data.availablePlugins[i];
			groupName = groupNameForPlugin(plugin);
			if ( result.groups[groupName] === undefined ) {
				result.groupNames.push(groupName);
				// note we assume plugins already sorted by name
				result.groups[groupName] = [];
			}
			result.groups[groupName].push(plugin);
		}
		result.groupNames.sort();
		for ( i = 0, len = data.installedPlugins.length; i < len; i++ ) {
			plugin = data.installedPlugins[i];
			result.installed[plugin.uid] = plugin;
		}
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
	
	var compareVersions = function(v1, v2) {
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
	
	var createPluginUI = function(plugin, installed) {
		var id = "Plugin-" +plugin.uid.replace(/\W/g, '-');
		var row = $('<div class="plugin clearfix"/>').attr('id', id).text(plugin.info.name);
		var actionContainer = $('<div class="pull-right"/>').appendTo(row);
		var installedPlugin = installed[plugin.uid];
		var button = undefined;
		if ( installedPlugin === undefined ) {
			// not installed; offer to install it
			button = $('<button class="btn btn-small btn-primary span2">').text('Install'); // TODO: l10n
			actionContainer.append(button);
		} else if ( compareVersions(plugin.version, installedPlugin.version) > 0 ) {
			// update available
			button = $('<button class="btn btn-small btn-info span2">').text('Upgrade'); // TODO: l10n
			actionContainer.append(button);
		} else {
			// installed
			button = $('<button class="btn btn-small btn-danger span2"/>').text('Remove');  // TODO: l10n
			actionContainer.append(button);
		}
		return row;
	};
	
	$.getJSON(url, function(data) {
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			container.empty();
			// TODO: l10n
			SolarNode.warn('Error!', 'An error occured loading plugin information.', container);
			return;
		}
		var i, iMax;
		var j, jMax;
		var groupedPlugins = groupPlugins(data.data);
		var html = $('<div class="accordion" id="plugin-list"/>');
		var groupBody = undefined;
		var groupName = undefined;
		var group = undefined;
		var plugin = undefined;
		for ( i = 0, iMax = groupedPlugins.groupNames.length; i < iMax; i++ ) {
			groupName = groupedPlugins.groupNames[i];
			groupBody = createGroup(groupName, html);
			group = groupedPlugins.groups[groupName];
			for ( j = 0, jMax = group.length; j < jMax; j++ ) {
				plugin = group[j];
				groupBody.append(createPluginUI(plugin, groupedPlugins.installed));
			}
		}
		container.html(html);
	});
};

$(document).ready(function() {
	$('#link-plugins').click(function(event) {
		event.preventDefault();

		var btn = $(this);
		if ( btn.hasClass('active') ) {
			// already active, nothing to do
			return;
		}

		// toggle other tabs off...
		$('.navbar li.active').removeClass('active');
		
		// make me active
		btn.parent().addClass('active');
		
		// replace body content with plugin UI
		var container = $('#body-container');
		container.html('<p>Loading...</p>');
		SolarNode.Plugins.populateUI(container);
	});
});
