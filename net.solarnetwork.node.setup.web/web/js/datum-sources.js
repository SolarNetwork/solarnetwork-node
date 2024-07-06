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
	 * @property {SettingsInfo} info the info
	 * @property {string[]} sourceIds - the source IDs
	 */

	/** @type Array<HostInfo> */

	
	const sourceIdTemplate = $('#datum-sources .template');
	const sourceIdContainer = $('#datum-sources .list-container');
	
	const sourceIdRows = new Map();
	
	function setupSourceIds(/** @type {DatumSourceInfo[]} */ infos) {
		sourceIdRows.clear();
		sourceIdContainer.empty();
		
		for ( let i = 0, len = infos.length; i < len; i += 1 ) {
			const ds = infos[i];
			if ( !Array.isArray(ds.sourceIds) || ds.sourceIds.length < 1 ) {
				continue;
			}
			for ( let j = 0, sLen = ds.sourceIds.length; j < sLen; j += 1 ) {
				renderSourceId(ds, j);
			}
		}

		$('#datum-sources .none').toggleClass('hidden', sourceIdRows.size > 0);
		$('#datum-sources .some').toggleClass('hidden', sourceIdRows.size < 1);
	}
	
	function renderSourceId(/** @type {DatumSourceInfo} */ ds, sourceIdx) {
		const itemEl = sourceIdTemplate.clone(true).removeClass('template');
		itemEl.data('datumSource', ds);
		if ( sourceIdx === 0 ) {
			itemEl.find('[data-tprop=displayName]').text(ds.info.displayName);
		}
		itemEl.find('[data-tprop=sourceId]').text(ds.sourceIds[sourceIdx]);
		
		sourceIdContainer.append(itemEl);
		sourceIdRows.set(ds.sourceIds[sourceIdx], itemEl);
	}
	
	/* ============================
	   Init
	   ============================ */
	(function initDatumSourcesManagement() {
		var loadCountdown = 1;
		
		/** @type {DatumSourceInfo[]} */
		var datumSources = [];
	
		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				setupSourceIds(datumSources);
			}
		}
	
		// list all known modes
		$.getJSON(SolarNode.context.path('/a/datum-sources/list'), function(json) {
			if ( json && json.success === true ) {
				console.debug('Got datum source list: %o', json.data);
				datumSources = json.data;
				liftoff();
			}
		});
		
		/*- TODO subscribe to get datum updates as they happen
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/cli');
		SolarNode.WebSocket.subscribeToTopic(topic, handleCliMessage);
		*/
	})();

});
