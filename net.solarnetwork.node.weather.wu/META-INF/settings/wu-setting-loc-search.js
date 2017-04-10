'use strict';

SolarNode.WeatherUnderground = {};

$(function() {
	var modal = $('#wu-location-lookup-modal'),
		form = modal.get(0),
		tbody = modal.find('tbody'),
		templateRow = modal.find('tr.template'),
		searchBtn = modal.find('button[type=submit]'),
		chooseBtn = modal.find('button.choose');
	
	SolarNode.WeatherUnderground.showLocationSearchResults = function(json) {
		//searchBtn.removeAttr('disabled');
		if ( Array.isArray(json.RESULTS) !== true ) {
			SolarNode.errorAlert("Error querying Weather Underground for locations: " +JSON.stringify(json));
			return;
		}
		var results = json.RESULTS;
		var i, len;
		var tr;
		var meta;
		tbody.empty();
		for ( i = 0, len = results.length; i < len; i++ ) {
			tr = templateRow.clone(true);
			tr.removeClass('template');
			meta = results[i];
			tr.data('locationMeta', meta);
			
			tr.children('td').each(function(idx, el) {
				var td = $(el);
				var prop = td.data('tprop');
				var val = SolarNode.extractJSONPath(meta, prop);
				if ( val ) {
					td.text(val);
				}
			});
			
			tbody.append(tr);
		}
		tbody.parent().removeClass('hidden');
	}
			
	// use JSONP to query
	modal.on('submit', function(event) {
		event.preventDefault();
		
		// start a spinner on the search button so we know a search is happening
		SolarNode.showLoading(searchBtn);
		
		// clear any previous selection
		chooseBtn.removeData('locationMeta');
		
		var url = modal.attr('action') +'?query=' +encodeURIComponent(form.elements['query'].value)
			+'&cb=' +encodeURIComponent(form.elements['cb'].value);
		
		$.getScript(url, function( data, textStatus, jqxhr ) {
			SolarNode.hideLoading(searchBtn);
		}).fail(function(xhr, settings, exception) {
			SolarNode.errorAlert("Error querying Weather Underground for locations: " +exception);
			SolarNode.hideLoading(searchBtn);
		});
	});
	
	$('.wu-loc-lookup-modal button.choose').on('click', function() {
		var me = $(this);
		var modal = me.closest('.modal');
		var selectedLocation = me.data('locationMeta');
		var currParams = me.data('params');
		if ( selectedLocation !== undefined ) {
			var settingId = currParams.settingId;
			// the location ID text field comes immediately before this ID, so subtract one
			var match = settingId.match(/^(.*i)(\d+)$/);
			var locSettingId = match[1] + String(Number(match[2]) - 1);
			$('input#'+locSettingId).val(selectedLocation.l).trigger('change');
		}
		modal.modal('hide');
	});

	$('button.wu-loc-search-open').on('click', function() {
		var container = $(this).parents('.setup-resource-container');
		chooseBtn.data('params', container.data());
		$('#wu-location-lookup-modal').modal('show');
	});
});
