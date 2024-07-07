$(document).ready(function datumSourcesManagement() {
	'use strict';

	if ( !$('#datum-sources').length ) {
		return;
	}
	
	/**
	 * Settings info information.
	 * 
	 * @typedef {Object} SettingsInfo
	 * @property {string} [settingUid] the setting UID
	 * @property {string} [uid] the UID
	 * @property {string} [groupUid] the group UID
	 * @property {string} [displayName] the display name
	 */

	/**
	 * Datum source information.
	 * 
	 * @typedef {Object} DatumSourceInfo
	 * @property {String} type the type of info, e.g. DatumDataSource, DatumFilterService, etc
	 * @property {String} identifier the component identifier
	 * @property {SettingsInfo} info the info
	 * @property {string[]} sourceIds - the source IDs
	 */

	/** @type Array<HostInfo> */

	
	const sourceIdTemplate = $('#datum-sources .template');
	const sourceIdContainer = $('#datum-sources .list-container');

	const datumDetailsTemplate = $('#datum-sources-datum-detail-modal .template');
	const datumDetailsContainer = $('#datum-sources-datum-detail-modal .list-container');
	
	const sourceIdRows = new Map();
	
	function setupSourceIds(/** @type {DatumSourceInfo[]} */ infos) {
		sourceIdRows.clear();
		sourceIdContainer.empty();
		
		const rows = [];
		
		for ( let i = 0, len = infos.length; i < len; i += 1 ) {
			const ds = infos[i];
			if ( !Array.isArray(ds.sourceIds) || ds.sourceIds.length < 1 ) {
				continue;
			}
			for ( let j = 0, sLen = ds.sourceIds.length; j < sLen; j += 1 ) {
				rows.push({
					key: ds.sourceIds[j],
					ds: ds,
					sourceIdx: j
				})
			}
		}

		rows.sort((l, r) => {
			return SolarNode.naturalSortCollator.compare(l.key, r.key);
		});
		for ( let row of rows ) {
			renderSourceId(row.ds, row.sourceIdx);
		}
		
		$('#datum-sources .none').toggleClass('hidden', sourceIdRows.size > 0);
		$('#datum-sources .some').toggleClass('hidden', sourceIdRows.size < 1);
	}

	function toggleLoading(on) {
		$('.init').toggleClass('hidden', !on);
		$('.ready').toggleClass('hidden', on);
	}
	
	function renderSourceId(/** @type {DatumSourceInfo} */ ds, sourceIdx) {
		const itemEl = sourceIdTemplate.clone(true).removeClass('template');
		itemEl.data('datumSource', ds);
		itemEl.find('[data-tprop=displayName]').text(ds.info.displayName);
		itemEl.find('[data-tprop=uid]').text(ds.info.uid);
		itemEl.find('[data-tprop=sourceId]').text(ds.sourceIds[sourceIdx]);
		itemEl.find('.datum-source-details-link').on('click', showDatumDetails);
		
		let link = SolarNode.context.path('/a/settings');
		if ( ds.type === 'DatumFilterService' ) {
			link += '/filters/manage?uid=';
		} else {
			link += '/manage?uid=';
		}
		link += encodeURIComponent(ds.info.settingUid);
		if ( ds.identifier ) {
			link += '#' + encodeURIComponent(ds.identifier);
		}
			
		itemEl.find('a.datum-source-link')
			.attr('href', link)
			;
		
		sourceIdContainer.append(itemEl);
		sourceIdRows.set(ds.sourceIds[sourceIdx], itemEl);
	}
	
	function showDatumDetails() {
		const btn = $(this);
		const datum = btn.data('datum');
		if ( !datum ) {
			return;
		}
		console.log('Show datum details: %o', datum);
		$('#datum-sources-datum-detail-modal').data('datum', datum).modal('show');
	}
	
	function handleDatumCapturedMessage(msg) {
		const datum = JSON.parse(msg.body).data,
			sourceId = datum ? datum.sourceId : undefined;
		if ( !sourceId ) {
			return;
		}
		const itemEl = sourceIdRows.get(sourceId);
		if ( !itemEl ) {
			return;
		}
		datum.created = moment(datum.created).format('YYYY-MM-DD HH:mm:ss.SSS');
		itemEl.find('.datum-source-details-link')
			.text(datum.created)
			.data('datum', datum)
			.removeClass('hidden')
			;
	}
	
	$('#datum-sources-datum-detail-modal')
		.on('show.bs.modal', function() {
			const modal = $(this)
				, datum = modal.data('datum');
			datumDetailsContainer.empty();
			const keys = ["created", "sourceId"];
			const keys2 = [];
			for ( let propName in datum ) {
				if ( propName.startsWith('_') || 'event.topics' === propName || keys.indexOf(propName) >= 0 ) {
					continue;
				}
				keys2.push(propName);
			}
			keys2.sort(SolarNode.naturalSortCollator.compare);
			for ( let propName of keys.concat(keys2) ) {
				const itemEl = datumDetailsTemplate.clone(true).removeClass('template');
				itemEl.find('[data-tprop=propName]').text(propName);
				itemEl.find('[data-tprop=propVal]').text(datum[propName]);
				datumDetailsContainer.append(itemEl);
			}
		});
	
	/* ============================
	   Init
	   ============================ */

 	$.getJSON(SolarNode.context.path('/a/datum-sources/list'), (data) => {
		if ( data && data.success === true ) {
			setupSourceIds(data.data);
			SolarNode.Datum.subscribeDatumCaptured(null, handleDatumCapturedMessage);
		}
	}).always(() => {
		toggleLoading(false);
	});
});
