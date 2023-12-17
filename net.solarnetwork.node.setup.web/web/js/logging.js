$(document).ready(function loggingManagement() {
	'use strict';

	if ( !$('#logging').length ) {
		return;
	}
	
	$('#edit-logger-level-modal').ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			$('#edit-logger-level-modal').find('button[type=submit]').prop('disabled', true);
			return true;
		},
		success: function(json, statusText, xhr, form) {
			if ( json && json.success === true ) {
				const logger = $(form.get(0).elements['logger']).val();
				const level = $(form.get(0).elements['level']).val();
				if ( editItemEl ) {
					if ( level === 'inherit' ) {
						// remove from list
						editItemEl.remove();
					} else {
						renderLevel(editItemEl, logger, level);
					}
				} else {
					const container = $('#logging-levels .list-container');
					const template = $('#logging-levels .template');
					appendLevel(container, template, logger, level);
				}
				form.modal('hide');
			} else {
				SolarNode.error(json.message, $('#edit-logger-level-modal .modal-body.start'));
			}
		},
		error: function(xhr) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, $('#edit-logger-level-modal .modal-body.start'));
		}
	})
	.on('show.bs.modal', function() {
		if ( editItemEl ) {
			let logger = editItemEl.data('logger');
			let level = editItemEl.data('level');
			$(this.elements['logger']).val(logger).prop('readonly', true);
			$(this.elements['level']).val(level.toLowerCase());
		}
		$(this).find('.create').toggleClass('hidden', !!editItemEl);
	})
	.on('shown.bs.modal', function() {
		$('#logging-logger-levels-logger').focus();
	})
	.on('hidden.bs.modal', function() {
		this.reset();
		$(this.elements['logger']).prop('readonly', false);
		$(this).find('button[type=submit]').prop('disabled', false);
		editItemEl = undefined;
	});
	
	$('#logging-loggers').on('change', (event) => {
		$('#logging-logger-levels-logger').val($(event.target).val());
	});
	
	var editItemEl;
	
	function renderLevel(itemEl, logger, level) {
		itemEl.data('logger', logger);
		itemEl.data('level', level);
		itemEl.find('[data-tprop=logger]').text(logger);
		itemEl.find('[data-tprop=level]').text(level.toUpperCase());
	}
	
	function appendLevel(container, template, logger, level) {
		var itemEl = template.clone(true).removeClass('template');
		renderLevel(itemEl, logger, level);
		itemEl.find('a').on('click', (event) => event.preventDefault());
		container.append(itemEl);
	}
	
	function renderLevels(levels) {
		const container = $('#logging-levels .list-container');
		const template = $('#logging-levels .template');
		
		const sortedLevels = Object.keys(levels);
		sortedLevels.sort();
		
		container.empty();
		
		sortedLevels.forEach(logger => {
			var level = levels[logger];
			appendLevel(container, template, logger, level);
		});
		
		container.on('click', (event) => {
			var itemEl = $(event.target).closest('.item');
			editItemEl = itemEl;
			$('#edit-logger-level-modal').modal('show');
		});
	}
	
	function renderLoggers(loggers) {
		const sel = $('#logging-loggers'),
			selEl = sel.get(0);
		sel.empty();
		selEl.add(new Option());
		loggers.forEach(logger => {
			selEl.add(new Option(logger,logger));
		});
	}
	
	/* ============================
	   Init
	   ============================ */
	(function initLoggingManagement() {
		var loadCountdown = 2;
		var levels = {};
		var loggers = [];

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				renderLevels(levels);
				renderLoggers(loggers);
			}
		}

		// list all configured levels
		$.getJSON(SolarNode.context.path('/a/logging/levels'), function(json) {
			if ( json && json.success === true ) {
				levels = json.data;
			}
			liftoff();
		});

		// list all active loggers
		$.getJSON(SolarNode.context.path('/a/logging/loggers'), function(json) {
			if ( json && json.success === true ) {
				loggers = json.data;
			}
			liftoff();
		});
	})();
	
});
