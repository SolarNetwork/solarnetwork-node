$(document).ready(function packageManagement() {
	'use strict';
	
	/**
	 * Package information.
	 * @typedef {Object} PlatformPackage
	 * @property {string} name - the package name
	 * @property {string} lcName - the package name, lower case
	 * @property {string} version - the package version
	 * @property {boolean} installed - true if installed
	 */
	
	/** @type Map<string, PlatformPackage> */
	const installed = new Map();
	
	/** @type Map<string, PlatformPackage> */
	const available = new Map();
	
	/** @type Map<string, PlatformPackage> */
	const upgradable = new Map();
	
		/** @type Map<string, string> */
	const morePackagesTemplate = $('#more-packages').children();

	if ( !$('#packages').length ) {
		return;
	}
	
	const installedSection = $('#packages-installed');
	const installedContainer = installedSection.find('.list-content');
	const installedTemplate = installedSection.find('.row.template');
	const installedFilter = $('#installedSearchFilter');

	const availableSection = $('#packages');
	const availableContainer = availableSection.find('.list-content');
	const availableTemplate = availableSection.find('.row.template');
	const availableFilter = $('#availableSearchFilter');

	const upgradableSection = $('#packages-upgradable');
	const upgradableContainer = upgradableSection.find('.list-content');
	const upgradableTemplate = upgradableSection.find('.row.template');

	const removeModal = $('#package-remove-modal');
	const installModal = $('#package-install-modal');
	const upgradeModal = $('#packages-upgrade-modal');
	
	const refreshButton = $('#packages-refresh');
	
	// A timer for keyup filter search
	let installedFilterTimer = undefined;
	
	// A timer for keyup filter search
	let availableFilterTimer = undefined;
		
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
	
	function populatePackageTemplateProperties(container, p) {
		container.find('[data-tprop=name]').text(p.name);
		container.find('[data-tprop=version]').text(p.version);
	}
	
	/**
	 * Render a package as HTML using a template.
	 * 
	 * @param {PlatformPackage} p - the package to render
	 * @param {jQuery} template - the template
	 * @param {jQuery} [container] - the destination container, or undefined to just test filter
	 * @param {string} [filter] - optional filter
	 * @returns {boolean} true if the package was rendered, false if filtered out
	 */
	function renderPackage(p, template, container, filter) {
		if (!!filter && !p.lcName.includes(filter)) {
			return false;
		}
		if ( container ) {
			const row = template.clone(true).removeClass('template');
			row.data('item', p);
			populatePackageTemplateProperties(row, p);
			container.append(row);
		}
		return true;
	}
	
	/**
	 * Filter the visible packages.
	 * 
	 * @param {jQuery} template - the template
	 * @param {jQuery} container - the destination container
	 * @param {string} [filter] - optional filter
	 * @param {Iterable<PlatformPackage>} packages - all possible packages
	 * @param {Map<string, PlatformPackage} mapping - map of package name to associated package
	 */
	function renderPackages(template, container, filter, packages, mapping) {
		container.empty();
		const c = $('<div class="row-list">');
		const max = 250;
		let shownCount = 0;
		let matchCount = 0;
		for ( const p of packages ) {
			if ( mapping ) {
				p.lcName = p.name.toLowerCase();
				mapping.set(p.name, p);
			}
			if (renderPackage(p, template, shownCount < max ? c : undefined, filter)) {
				matchCount += 1;
				if ( shownCount < max ) {
					shownCount += 1;
				}
			}
		}
		if ( shownCount === max && matchCount > max ) {
			const more = morePackagesTemplate.clone(true);
			more.find('.count').text(matchCount - shownCount);
			c.append(more);
		}
		container.append(c);
	}
	
	function reRenderPackages() {
		renderPackages(installedTemplate, installedContainer, installedFilter.val().toLowerCase(), installed.values());
		renderPackages(availableTemplate, availableContainer, availableFilter.val().toLowerCase(), available.values());
		renderPackages(upgradableTemplate, upgradableContainer, undefined, upgradable.values());
	}
	
	function handlePackageClick(event) {
		event.preventDefault();
		const btn = $(event.target);
		const item = btn.closest('.row').data('item');
		const name = item ? item.name : undefined;
		if ( !name ) {
			return;
		}
		console.debug('Click on package %s', item.name);
		const modal = installedContainer.has(btn).length ? removeModal : installModal;
		modal.data('item', item);
		modal.modal('show');
	}
	
	/**
	 * Handle a package list response.
	 * 
	 * @param {Object} data - the response
	 * @param {boolean} data.success - true if the request was processed
	 * @param {Object} [data.data] - the package data object
	 * @param {Array<PlatformPackage>} [data.data.installedPackages] - the installed package list
	 * @param {Array<PlatformPackage>} [data.data.availablePackages] - the available package list
	 * @param {Array<PlatformPackage>} [data.data.upgradablePackages] - the upgradable package list
	 */
	function handlePackageListResponse(data) {
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			SolarNode.warn('Error!', 'An error occured loading package information.');
			return;
		}
		installed.clear();
		available.clear();
		upgradable.clear();
		if ( Array.isArray(data.data.installedPackages) ) {
			renderPackages(installedTemplate, installedContainer, installedFilter.val().toLowerCase(),
					data.data.installedPackages, installed);
		}
		if ( Array.isArray(data.data.availablePackages) ) {
			renderPackages(availableTemplate, availableContainer, availableFilter.val().toLowerCase(),
					data.data.availablePackages, available);
		}
		if ( Array.isArray(data.data.upgradablePackages) ) {
			renderPackages(upgradableTemplate, upgradableContainer, undefined,
					data.data.upgradablePackages, upgradable);
			if ( upgradable.size ) {
				upgradableSection.removeClass('hidden');
			}
		}
	}

	/** Install package modal. */
	installModal.ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			SolarNode.showLoading(installModal.find('button[type=submit]'));
			return true;
		},
		success: function(json) {
			SolarNode.hideLoading(installModal.find('button[type=submit]'));
			if ( json && json.success === true && json.data && json.data.success ) {
				toggleBeforeAfter(installModal, false, true);
				const p = installModal.data('item');
				p.installed = true;
				available.delete(p.name);
				if ( upgradable.has(p.name) ) {
					upgradable.delete(p.name);
					if ( upgradable.size < 1 ) {
						upgradableSection.addClass('hidden');
					}
				}
				installed.set(p.name, p);
				setTimeout(reRenderPackages, 50);
			} else {
				const msg = json.data && json.data.message ? json.data.message : json.message;
				installModal.find('.error-message').text(msg);
				toggleBeforeAfter(installModal, false, false);
			}
		},
		error: function(xhr) {
			const msg = SolarNode.extractResponseErrorMessage(xhr);
			installModal.find('.error-message').text(msg);
			toggleBeforeAfter(installModal, false, false);
			SolarNode.hideLoading(installModal.find('button[type=submit]'));
		}
	})
	.on('show', function() {
		const p = installModal.data('item');
		populatePackageTemplateProperties(installModal.find('.modal-body'), p);
		installModal.find('input[name=name]').val(p.name);
	})
	.on('hidden', function() {
		toggleBeforeAfter(installModal, true);
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
			if ( json && json.success === true && json.data && json.data.success ) {
				toggleBeforeAfter(removeModal, false, true);
				const p = removeModal.data('item');
				p.installed = false;
				installed.delete(p.name);
				if ( upgradable.has(p.name) ) {
					available.set(p.name, upgradable.get(p.name));
					upgradable.delete(p.name);
					if ( upgradable.size < 1 ) {
						upgradableSection.addClass('hidden');
					}
				} else {
					available.set(p.name, p);
				}
				setTimeout(reRenderPackages, 50);
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
	.on('show', function() {
		const p = removeModal.data('item');
		populatePackageTemplateProperties(removeModal.find('.modal-body'), p);
		removeModal.find('input[name=name]').val(p.name);
	})
	.on('hidden', function() {
		toggleBeforeAfter(removeModal, true);
		this.reset();
	});
	
	/** Upgrade all package modal. */
	upgradeModal.ajaxForm({
		dataType: 'json',
		beforeSubmit: function() {
			SolarNode.showLoading(upgradeModal.find('button[type=submit]'));
			return true;
		},
		success: function(json) {
			if ( json && json.success === true && json.data && json.data.success ) {
				toggleBeforeAfter(upgradeModal, false, true);
				for ( const [n, p] of upgradable ) {
					installed.set(n, p);
				}
				upgradable.clear();
				upgradableSection.addClass('hidden');
				setTimeout(reRenderPackages, 50);
			} else {
				const msg = json.data && json.data.message ? json.data.message : json.message;
				upgradeModal.find('.error-message').text(msg);
				toggleBeforeAfter(upgradeModal, false, false);
			}
			SolarNode.hideLoading(upgradeModal.find('button[type=submit]'));
		},
		error: function(xhr) {
			const msg = SolarNode.extractResponseErrorMessage(xhr);
			upgradeModal.find('.error-message').text(msg);
			toggleBeforeAfter(upgradeModal, false, false);
			SolarNode.hideLoading(upgradeModal.find('button[type=submit]'));
		}
	})
	.on('hidden', function() {
		toggleBeforeAfter(upgradeModal, true);
		this.reset();
	});
	
	$('form.packages button.restart').on('click', (event) => {
		event.preventDefault();
		$(event.target.form).modal('hide');
		$('#restart-modal').modal('show');
	});
	
	installedFilter.on('keyup', (event) => {
		if (installedFilterTimer) {
			clearTimeout(installedFilterTimer);
		}
		const filter = event.target.value;
		installedFilterTimer = setTimeout(() => {
			renderPackages(installedTemplate, installedContainer, filter.toLowerCase(), installed.values());
		}, 500);
	});
	
	installedContainer.on('click', handlePackageClick);
	
	availableFilter.on('keyup', (event) => {
		if (availableFilterTimer) {
			clearTimeout(availableFilterTimer);
		}
		const filter = event.target.value;
		availableFilterTimer = setTimeout(() => {
			renderPackages(availableTemplate, availableContainer, filter.toLowerCase(), available.values());
		}, 500);
	});
	
	availableContainer.on('click', handlePackageClick);
	
	upgradableContainer.on('click', handlePackageClick);
	
	// Refresh package list
	refreshButton.on('click', () => {
		SolarNode.showLoading(refreshButton);
		$.getJSON(SolarNode.context.path('/a/packages/refresh'), (data) => {
			handlePackageListResponse(data);
		}).always(() => {
			SolarNode.hideLoading(refreshButton);
		});
	});
	
	// Initialize package list
	$.getJSON(SolarNode.context.path('/a/packages/list'), (data) => {
		$('.init').addClass('hidden');
		handlePackageListResponse(data);
	}).always(() => {
		toggleLoading(false);
	});
});