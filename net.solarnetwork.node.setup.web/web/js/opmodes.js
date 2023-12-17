$(document).ready(function opModesManagement() {
	'use strict';

	if ( !$('#opmodes').length ) {
		return;
	}
	
	const activeModes = [];
	const activeModesContainer = $('#opmodes-active .list-container');
	const activeModesTemplate = $('#opmodes-active .template');
	
	function renderModes(container, template, modes) {
		const newModes = (Array.isArray(modes) ? modes : []);
		newModes.sort();
		
		// check if new modes are actually same as what we have; return if so
		if ( newModes.length == activeModes.length ) {
			let same = true;
			for ( let i = 0, len = activeModes.length; i < len; i += 1 ) {
				if ( activeModes[i] !== modes[i] ) {
					same = false;
					break;
				}
			}
			if ( same ) {
				return;
			}
		}
		
		// replace activeModes with modes
		Array.prototype.splice.apply(activeModes, [0, activeModes.length].concat(newModes));
		if ( newModes.length > 0 ) {
			container.empty();
			newModes.forEach(mode => {
				appendMode(container, template, mode);
			});
		}
		$('.opmodes-none').toggleClass('hidden', newModes.length > 0);
		$('.opmodes-some').toggleClass('hidden', newModes.length < 1);
	}
	
	function appendMode(container, template, mode) {
		var itemEl = template.clone(true).removeClass('template');
		renderMode(itemEl, mode);
		container.append(itemEl);
	}
	
	function renderMode(itemEl, mode) {
		itemEl.data('mode', mode);
		itemEl.find('[data-tprop=mode]').text(mode);
	}
	
	function handleOperationalModesChangeMessage(msg) {
		const evt = JSON.parse(msg.body).data;
		console.log('Got OpModes change event: %o', evt)
		const modes = (Array.isArray(evt.ActiveOpModes) ? evt.ActiveOpModes : []);
		renderModes(activeModesContainer, activeModesTemplate, modes);
	}
	
	$('#add-opmodes-modal').ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			$('#add-opmodes-modal').find('button[type=submit]').prop('disabled', true);
			return true;
		},
		success: function(json, statusText, xhr, form) {
			if ( json && json.success === true ) {
				renderModes(activeModesContainer, activeModesTemplate, json.data);
				form.modal('hide');
			} else {
				SolarNode.error(json.message, $('#add-opmodes .modal-body.start'));
			}
		},
		error: function(xhr) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, $('#add-opmodes .modal-body.start'));
		}
	})
	.on('shown.bs.modal', function() {
		$('#add-opmodes-modal input[name=modes]').focus();
	})
	.on('hidden.bs.modal', function() {
		this.reset();
		$(this).find('button[type=submit]').prop('disabled', false);
	});
	
	$('#opmodes-active').on('click', function handleOpModesClick(event) {
		event.preventDefault();
		const target = $(event.target).closest('button');
		const mode = target.closest('.item').data('mode');
		if ( target.attr('name') === 'delete' && mode ) {
			$.ajax({
				type: 'DELETE',
				dataType: 'json',
				url: SolarNode.context.path('/a/opmodes/active'),
				beforeSend: (xhr) => SolarNode.csrf(xhr),
				contentType: 'application/json',
				data: JSON.stringify([mode]),
			}).then((json) => {
				renderModes(activeModesContainer, activeModesTemplate, json.data);
			}, (xhr, status) => {
				console.log("Failed to delete mode [%s]: %s", mode, status);
			});
		}
	});

	/* ============================
	   Init
	   ============================ */
	(function initOpModesManagement() {
		// list all active modes
		$.getJSON(SolarNode.context.path('/a/opmodes/active'), function(json) {
			if ( json && json.success === true ) {
				renderModes(activeModesContainer, activeModesTemplate, json.data);
			}
		});
		
		//subscribe to get mode changes as they happen
		var topic = '/topic/OperationalModesService/MODES_CHANGED';
		SolarNode.WebSocket.subscribeToTopic(topic, handleOperationalModesChangeMessage);
	})();
	
});
