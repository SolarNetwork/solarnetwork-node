$(document).ready(function hostManagement() {
	'use strict';
	
	/**
	 * Host information.
	 * @typedef {Object} HostInfo
	 * @property {string} key - the alias hostname
	 * @property {string} value - the IP address
	 */
	
	/** @type Array<HostInfo> */
	const hosts = [];
	
	if ( !$('#hosts').length ) {
		return;
	}
	
	const hostsSection = $('#hosts');
	const hostsContainer = hostsSection.find('.list-content');
	const hostsTemplate = hostsSection.find('.row.template');

	const addModal = $('#host-add-modal');
	const removeModal = $('#host-remove-modal');
	
	function toggleLoading(on) {
		$('.init').toggleClass('hidden', !on);
		$('.ready').toggleClass('hidden', on);
	}
	
	/**
	 * Toggle the visibility of before/after and success/error elements.
	 * 
	 * @param {jQuery} container - the element to operate on
	 * @param {boolean} before - true if before, false if after
	 * @param {boolean} [success] - if provided and before is false, toggle success/error elements
	 */
	function toggleBeforeAfter(container, before, success) {
		container.find('.before').toggleClass('hidden', !before);
		container.find('.after').toggleClass('hidden', before);
		if ( !before && success !== undefined ) {
			container.find('.success').toggleClass('hidden', !success);
			container.find('.error').toggleClass('hidden', success);
		}
	}
	
	/**
	 * Populate host information.
	 * 
	 * @param {jQuery} container - the HTML to update
	 * @param {HostInfo} info - the host info
	 */
	function populateHostTemplateProperties(container, info) {
		container.find('[data-tprop=name]').text(info.key);
		container.find('[data-tprop=address]').text(info.value);
	}
	
	/**
	 * Render a host as HTML using a template.
	 * 
	 * @param {HostInfo} info - the package to render
	 * @param {jQuery} template - the template
	 * @param {jQuery} container - the destination container
	 */
	function renderHost(info, template, container) {
		const row = template.clone(true).removeClass('template');
		row.data('item', info);
		populateHostTemplateProperties(row, info);
		container.append(row);
	}
	
	/**
	 * Render the hosts.
	 * 
	 * @param {jQuery} template - the template
	 * @param {jQuery} container - the destination container
	 * @param {Iterable<HostInfo>} hosts - all possible packages
	 * @param {Array<HostInfo>} [list] - a list to copy the hosts to
	 * @param {Map<string, PlatformPackage} mapping - map of package name to associated package
	 */
	function renderHosts(template, container, hosts, list) {
		container.empty();
		const c = $('<div class="row-list">');
		for ( const host of hosts ) {
			renderHost(host, template, c);
			if ( list ) {
				list.push(host);
			}
		}
		container.append(c);
	}
	
	function reRenderHosts() {
		renderHosts(hostsTemplate, hostsContainer, hosts);
	}
	
	function handleHostClick(event) {
		event.preventDefault();
		const btn = $(event.target);
		const item = btn.closest('.row').data('item');
		const alias = item ? item.key : undefined;
		if ( !alias ) {
			return;
		}
		console.debug('Click on host %s', alias);
		const modal = hostsContainer.has(btn).length ? removeModal : addModal;
		modal.data('item', item);
		modal.modal('show');
	}
	
	/**
	 * Handle a host list response.
	 * 
	 * @param {Object} data - the response
	 * @param {boolean} data.success - true if the request was processed
	 * @param {Array<HostInfo>} [data.data] - the hosts list
	 */
	function handleHostListResponse(data) {
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			SolarNode.warn('Error!', 'An error occured loading package information.');
			return;
		}
		hosts.length = 0;
		if ( Array.isArray(data.data) ) {
			renderHosts(hostsTemplate, hostsContainer, data.data, hosts);
		}
	}

	/** Add package modal. */
	addModal.ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			SolarNode.showLoading(addModal.find('button[type=submit]'));
			return true;
		},
		success: function(json) {
			SolarNode.hideLoading(addModal.find('button[type=submit]'));
			if ( json && json.success === true && json.data ) {
				hosts.push(json.data);
				setTimeout(reRenderHosts, 50);
				addModal.modal('hide');
			} else {
				const msg = json.data && json.data.message ? json.data.message : json.message;
				addModal.find('.error-message').text(msg);
				toggleBeforeAfter(addModal, false, false);
			}
		},
		error: function(xhr) {
			const msg = SolarNode.extractResponseErrorMessage(xhr);
			addModal.find('.error-message').text(msg);
			toggleBeforeAfter(addModal, false, false);
			SolarNode.hideLoading(addModal.find('button[type=submit]'));
		}
	})
	.on('shown.bs.modal', function() {
		addModal.find('input[name=name]').focus();
	})
	.on('hidden.bs.modal', function() {
		toggleBeforeAfter(addModal, true);
		this.reset();
	});
	
	/** Remove package modal. */
	removeModal.ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			SolarNode.showLoading(removeModal.find('button[type=submit]'));
			return true;
		},
		success: function(json) {
			if ( json && json.success === true ) {
				const info = removeModal.data('item');
				const idx = hosts.indexOf(info);
				if ( idx >= 0 ) {
					hosts.splice(idx, 1);
				}
				setTimeout(reRenderHosts, 50);
				removeModal.modal('hide');
			} else {
				const msg = json.data && json.data.message ? json.data.message : json.message;
				removeModal.find('.error-message').text(msg);
				toggleBeforeAfter(removeModal, false, false);
			}
			SolarNode.hideLoading(removeModal.find('button[type=submit]'));
		},
		error: function(xhr) {
			const msg = SolarNode.extractResponseErrorMessage(xhr);
			removeModal.find('.error-message').text(msg);
			toggleBeforeAfter(removeModal, false, false);
			SolarNode.hideLoading(removeModal.find('button[type=submit]'));
		}
	})
	.on('show.bs.modal', function() {
		const host = removeModal.data('item');
		populateHostTemplateProperties(removeModal.find('.modal-body'), host);
		removeModal.find('input[name=name]').val(host.key);
	})
	.on('hidden.bs.modal', function() {
		toggleBeforeAfter(removeModal, true);
		this.reset();
	});
	
	
	hostsContainer.on('click', handleHostClick);
	
	// Initialize package list
	$.getJSON(SolarNode.context.path('/a/hosts/list'), (data) => {
		handleHostListResponse(data);
	}).always(() => {
		toggleLoading(false);
	});
});