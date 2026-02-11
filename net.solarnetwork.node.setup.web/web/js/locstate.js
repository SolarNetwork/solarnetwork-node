$(document).ready(function localStateManagement() {
	'use strict';

	if ( !$('#local-state').length ) {
		return;
	}
	
	/**
	 * LocalState info information.
	 * 
	 * @typedef {Object} LocalStateInfo
	 * @property {string} key the identifier
	 * @property {string} created the creation date
	 * @property {string} modified the modification date
	 * @property {string} type the data type
	 * @property {string} [typeDisplay] the data type to display
	 * @property {any} value the data value
	 * @property {any} valueDisplay the data value to display
	 */

	const localStateTemplate = $('#local-state .template');
	const localStateContainer = $('#local-state .list-container');
	const localStateModal = $('#local-state-edit-modal');

	/** @type {Map<string, LocalStateInfo>} */
	const localStateRows = new Map();
	
	function setupLocalState(/** @type {LocalStateInfo[]} */ infos) {
		localStateRows.clear();
		localStateContainer.empty();
		
		const rows = [];
		
		for ( let i = 0, len = infos.length; i < len; i += 1 ) {
			const info = infos[i];
			if ( !info.key ) {
				continue;
			}
			rows.push(info);
		}

		rows.sort((l, r) => {
			return SolarNode.naturalSortCollator.compare(l.key, r.key);
		});
		for ( let row of rows ) {
			renderLocalState(row);
		}
		
		toggleRowsVisible();
	}
	
	function toggleRowsVisible() {
		$('#local-state .none').toggleClass('hidden', localStateRows.size > 0);
		$('#local-state .some').toggleClass('hidden', localStateRows.size < 1);
	}

	function toggleLoading(on) {
		$('.init').toggleClass('hidden', !on);
		$('.ready').toggleClass('hidden', on);
	}
	
	function renderDisplayValue(/** @type {LocalStateInfo} */ info) {
		return typeof info.value === 'object' 
			? JSON.stringify(info.value, undefined, 2)
			: info.value;
	}
	
	const DISPLAY_DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';
	
	function renderLocalState(/** @type {LocalStateInfo} */ info) {
		const typeDisplay = info.type;
		const valueDisplay = renderDisplayValue(info);

		const itemEl = localStateRows.has(info.key) 
			? localStateRows.get(info.key).removeClass('brief-showcase')
			: localStateTemplate.clone(true).removeClass('template');
		itemEl.find('[data-tprop=key]').text(info.key);
		itemEl.find('[data-tprop=createdDate]').text(moment(info.created).format(DISPLAY_DATE_FORMAT));
		itemEl.find('[data-tprop=modifiedDate]').text(moment(info.modified).format(DISPLAY_DATE_FORMAT));
		itemEl.find('[data-tprop=typeDisplay]').text(typeDisplay);
		itemEl.find('[data-tprop=valueDisplay]').text(valueDisplay);
		itemEl.find('.edit-link').data('localState', info);
		

		if ( !localStateRows.has(info.key) ) {
			itemEl.find('.edit-link').on('click', showLocalStateEditForm);
			localStateContainer.append(itemEl);
			localStateRows.set(info.key, itemEl);
		} else {
			setTimeout(() => {
				// kick to another event loop
				itemEl.addClass('brief-showcase');
			}, 100);
		}
	}
	
	function showLocalStateEditForm() {
		const btn = $(this);
		const info = btn.data('localState');
		if ( !info ) {
			return;
		}
		console.log('Edit local state: %o', info);
		localStateModal.data('localState', info).modal('show');
	}
	
	localStateModal
		.ajaxForm({
			dataType: 'json',
			beforeSubmit: function() {
				localStateModal.find('button[type=submit]').prop('disabled', true);
				return true;
			},
			success: function(json, _statusText, _xhr, form) {
				if ( json && json.success === true ) {
					if ( json.data && json.data.key ) {
						renderLocalState(json.data);
						toggleRowsVisible();
					}
					form.modal('hide');
				} else {
					SolarNode.error(json.message, $('#local-state-edit-modal .modal-body.start'));
				}
			},
			error: function(xhr) {
				var json = $.parseJSON(xhr.responseText);
				SolarNode.error(json.message, $('#local-state-edit-modal .modal-body.start'));
			}
		})
		.on('show.bs.modal', function() {
			const modal = $(this)
				, info = modal.data('localState');
			if (info) {
				modal.find('[name=key]').val(info.key);
				modal.find('[name=type]').val(info.type);
				modal.find('[name=value]').val(renderDisplayValue(info));
			}
			modal.find('[name=delete]').prop('disabled', !info);
		})
		.on('shown.bs.modal', function() {
			$('#edit-local-state-modal-key').focus();
		})
		.on('hidden.bs.modal', function() {
			this.reset();
			$(this).removeData('localState')
				.find('button[type=submit]')
				.prop('disabled', false);
		});

	localStateModal.find('button[name=delete]').on('click', function handleLocalStateDelete() {
		const form = localStateModal[0];
		const key = form.elements.key.value;
		$.ajax({
			type: 'DELETE',
			dataType: 'json',
			url: form.action + '?key=' +encodeURIComponent(key),
			beforeSend: (xhr) => SolarNode.csrf(xhr),
		}).then(() => {
			const itemEl = localStateRows.get(key);
			if ( itemEl ) {
				localStateRows.delete(key);
				itemEl.remove();
				toggleRowsVisible();
			}
			localStateModal.modal('hide');
		}, (_xhr, status) => {
			console.log("Failed to delete local state [%s]: %s", key, status);
		});
	});

	function handleLocalStateUpdatedMessage(msg) {
		/** @type LocalStateInfo */
		const data = JSON.parse(msg.body).data;
		if ( !(data && data.entity) ) {
			return;
		}
		console.debug('LocalState stored: %o', data.entity);
		renderLocalState(data.entity);
		toggleRowsVisible();
	}
	
	/* ============================
	   Init
	   ============================ */

 	$.getJSON(SolarNode.context.path('/a/local-state/list'), (data) => {
		if ( data && data.success === true ) {
			setupLocalState(data.data);
		}
	}).always(() => {
		toggleLoading(false);
	});
	
	// subscribe to get LocalState updates as they happen
	SolarNode.WebSocket.subscribeToTopic('/topic/dao/LocalState/STORED', handleLocalStateUpdatedMessage);
});
