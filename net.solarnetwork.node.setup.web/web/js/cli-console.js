$(document).ready(function cliConsoleManagement() {
	'use strict';

	if ( !$('#cli-console').length ) {
		return;
	}
	
	const knownTypes = new Set();
	const activeTypes = new Set();
	
	const typeTemplate = $('#cli-console-types .template');
	const typeContainer = $('#cli-console-types .list-container');

	const commandTemplate = $('#cli-console .template');
	const commandContainer = $('#cli-console .list-container');
	
	const clearCliCommandsButton = $('#cli-console-clear-logging');
	const loggingActiveToggle = $('#cli-console-logging-toggle');
	
	var loggingActive = true;
	
	function handleCliMessage(msg) {
		console.debug('Got CLI message: %o', msg.body);
		if ( !loggingActive ) {
			return;
		}
		const cmd = JSON.parse(msg.body);
		if ( Array.isArray(cmd) ) {
			addCliCommand(cmd);
		}
	}
	
	function addCliCommand(cmd) {
		const itemEl = commandTemplate.clone(true).removeClass('template');
		const cmdDisplay = cmd.join(' ');
		itemEl.attr('title', moment().format('YYYY-MM-DD HH:mm:ss'));
		itemEl.find('[data-tprop=command]').text(cmdDisplay);
		commandContainer.prepend(itemEl);
		clearCliCommandsButton.prop('disabled', false);
	}
	
	function clearCliCommands() {
		commandContainer.empty();
		clearCliCommandsButton.prop('disabled', true);
	}
	
	function handleCliCommandClick(event) {
		const btn = $(event.target).closest('button');
		if ( btn.hasClass('copy') )  {
			event.preventDefault();
			const cmd = btn.closest('.item').find('.cmd');
			if ( SolarNode.copyElementValue(cmd) ) {
				const icon = btn.find('i');
				icon.addClass('bi bi-clipboard2-check').removeClass('bi bi-clipboard2');
				setTimeout(() => {
					icon.addClass('bi bi-clipboard2').removeClass('bi bi-clipboard2-check');
				}, 3000);
			}
		}
	}

	function setupTypes(known, active) {
		known.sort();
		knownTypes.clear();
		known.forEach(knownTypes.add, knownTypes);
		
		active.sort();
		activeTypes.clear();
		active.forEach(activeTypes.add, activeTypes);
		
		typeContainer.empty();
		for ( let i = 0, len = known.length; i < len; i += 1 ) {
			addType(known[i]);
		}
		
		$('#cli-console-types .none').toggleClass('hidden', known.length > 0);
		$('#cli-console-types .some').toggleClass('hidden', known.length < 1);
	}
	
	function updateActive(active, type) {
		active.sort();
		activeTypes.clear();
		active.forEach(activeTypes.add, activeTypes);
		typeContainer.children().each(function() {
			setupTypeToggle($(this), type);
		});
	}
	
	function addType(type) {
		const itemEl = typeTemplate.clone(true).removeClass('template');
		itemEl.data('type', type);
		itemEl.find('[data-tprop=name]').text(type);
		typeContainer.append(itemEl);
		
		const toggle = setupTypeToggle(itemEl, type);
		
		toggle.button();
		toggle.on('click', () => {
			toggle.button('toggle');
			const active = toggle.hasClass('active');
			toggle.text(active ? toggle.data('labelOn') : toggle.data('labelOff'));
			toggle.toggleClass('btn-success', active);
			toggle.toggleClass('btn-secondary', !active);
			toggleType(type, active);
		});
	}
	
	function setupTypeToggle(itemEl, type) {
		const toggle = itemEl.find('button.toggle');
		if ( activeTypes.has(type) ) {
			toggle.addClass('btn-success active');
			toggle.text(toggle.data('labelOn'));
		}
		return toggle;
	}
	
	function toggleType(type, active) {
		$.ajax({
			type: 'POST',
			dataType: 'json',
			url: SolarNode.context.path('/a/cli-console/logging'),
			beforeSend: (xhr) => SolarNode.csrf(xhr),
			data:{
				types: type,
				enabled: active
			},
		}).then((json) => {
			if ( json.success ) {
				updateActive(json.data, type);
			}
		}, (xhr, status) => {
			console.warn("Failed to toggle CLI Command logging type [%s] => %s: %s", mode, enabled, status);
		});
	}
	
	function toggleLoggingActive() {
		loggingActive = !loggingActive;
		loggingActiveToggle.attr('title', loggingActive 
			? loggingActiveToggle.data('labelOn') 
			: loggingActiveToggle.data('labelOff'));
		loggingActiveToggle.toggleClass('btn-success', loggingActive);
		loggingActiveToggle.toggleClass('btn-info', !loggingActive);
		loggingActiveToggle.find('i')
			.addClass(loggingActive
				? loggingActiveToggle.data('classOn')
				: loggingActiveToggle.data('classOff'))
			.removeClass(!loggingActive
				? loggingActiveToggle.data('classOn')
				: loggingActiveToggle.data('classOff'));
	}

	/* ============================
	   Init
	   ============================ */
	(function initCliConsoleManagement() {
		var loadCountdown = 2;
		var known = [];
		var active = [];

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				setupTypes(known, active);
			}
		}

		// list all known modes
		$.getJSON(SolarNode.context.path('/a/cli-console/types'), function(json) {
			if ( json && json.success === true ) {
				console.debug('Got known CLI Command logging types: %o', json.data);
				known = json.data;
				liftoff();
			}
		});
		
		// list all active modes
		$.getJSON(SolarNode.context.path('/a/cli-console/logging'), function(json) {
			if ( json && json.success === true ) {
				console.debug('Got active CLI Command logging types: %o', json.data);
				active = json.data;
				liftoff();
			}
		});
		
		//subscribe to get CLI Commands as they happen
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/cli');
		SolarNode.WebSocket.subscribeToTopic(topic, handleCliMessage);
		
		commandContainer.on('click', handleCliCommandClick);
		clearCliCommandsButton.on('click', clearCliCommands);
		
		loggingActiveToggle.button().on('click', toggleLoggingActive);
	})();
	
});
