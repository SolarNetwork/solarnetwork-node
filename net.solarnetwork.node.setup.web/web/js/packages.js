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
	
	if ( !$('#packages').length ) {
		return;
	}
	
	const installedSection = $('#package-installed');
	const installedContainer = installedSection.find('.list-content');
	const installedTemplate = installedSection.find('.row.template');
	const installedFilter = $('#installedSearchFilter');

	const availableSection = $('#packages');
	const availableContainer = availableSection.find('.list-content');
	const availableTemplate = availableSection.find('.row.template');
	const availableFilter = $('#availableSearchFilter');

	// A timer for keyup filter search
	let installedFilterTimer = undefined;
	
	// A timer for keyup filter search
	let availableFilterTimer = undefined;
	
	function toggleLoading(on) {
		$('.init').toggleClass('hide', !on);
		$('.ready').toggleClass('hide', on);
	}
	
	/**
	 * Render a package as HTML using a template.
	 * 
	 * @param {PlatformPackage} p - the package to render
	 * @param {jQuery} template - the template
	 * @param {jQuery} container - the destination container
	 * @param {string} [filter] - optional filter
	 */
	function renderPackage(p, template, container, filter) {
		if (!!filter && !p.lcName.includes(filter)) {
			return;
		}
		const row = template.clone(true).removeClass('template');
		row.data('item', p);
		row.find('[data-tprop=name]').text(p.name);
		row.find('[data-tprop=version]').text(p.version);
		container.append(row);
	}
	
	/**
	 * Filter the visible packages.
	 * 
	 * @param {jQuery} template - the template
	 * @param {jQuery} container - the destination container
	 * @param {string} [filter] - optional filter
	 * @param {Map<string, PlatformPackage>} data - all possible packages
	 */
	function renderPackages(template, container, filter, data) {
		container.empty();
		const c = $('<div>');
		for ( const p of data.values() ) {
			renderPackage(p, template, c, filter);
		}
		container.append(c);
	}
	
	/** INIT */
	
	$.getJSON(SolarNode.context.path('/a/packages/list'), function(data) {
		$('.init').addClass('hide');
		if ( data === undefined || data.success !== true || data.data === undefined ) {
			// TODO: l10n
			SolarNode.warn('Error!', 'An error occured loading package information.');
			return;
		}
		if ( Array.isArray(data.data.installedPackages) ) {
			const c = $('<div>');
			for ( const p of data.data.installedPackages ) {
				p.lcName = p.name.toLowerCase();
				installed.set(p.name, p);
				renderPackage(p, installedTemplate, c, installedFilter.val().toLowerCase());
			}
			installedContainer.append(c);
		}
		if ( Array.isArray(data.data.availablePackages) ) {
			const c = $('<div>');
			for ( const p of data.data.availablePackages ) {
				p.lcName = p.name.toLowerCase();
				available.set(p.name, p);
				renderPackage(p, availableTemplate, c, availableFilter.val().toLowerCase());
			}
			availableContainer.append(c);
		}
		toggleLoading(false);
	});
	
	installedFilter.on('keyup', (event) => {
		if (installedFilterTimer) {
			clearTimeout(installedFilterTimer);
		}
		const filter = event.target.value;
		installedFilterTimer = setTimeout(() => {
			renderPackages(installedTemplate, installedContainer, filter.toLowerCase(), installed);
		}, 500);
	});
	
	availableFilter.on('keyup', (event) => {
		if (availableFilterTimer) {
			clearTimeout(availableFilterTimer);
		}
		const filter = event.target.value;
		availableFilterTimer = setTimeout(() => {
			renderPackages(availableTemplate, availableContainer, filter.toLowerCase(), available);
		}, 500);
	});
});