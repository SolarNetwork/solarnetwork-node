'use strict';

SolarNode.WeatherUnderground = {};

$(function() {
	var modal = $('#wu-location-lookup-modal'),
		form = modal.get(0),
		tbody = modal.find('tbody'),
		templateRow = modal.find('tr.template'),
		searchBtn = modal.find('button[type=submit]'),
		chooseBtn = modal.find('button.choose');
	
	var activeContainer;
	
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
			var locNameEl = activeContainer.find('.wu-loc-id'),
				settingId = currParams.settingId,
				// the location ID text field comes immediately before this ID, so subtract one
				match = settingId.match(/^(.*i)(\d+)$/),
				locSettingId = match[1] + String(Number(match[2]) - 1);
			locNameEl.text(selectedLocation.name);
			$('input#'+locSettingId).val(selectedLocation.l).trigger('change');
		}
		modal.modal('hide');
	});
	
	function wuContainer(el) {
		return $(el).parents('.setup-resource-container');
	}
	
	function showLocationNameResult(json, el) {
		if ( !json || !json.location ) {
			return;
		}
		var c = json.location.country,
			cName = json.location.country_name,
			st = json.location.state,
			l = json.location.city,
			name;
		if ( c == 'US' ) {
			name = l +', ' +st;
		} else {
			name = l + ', ' +cName;
		}
		$(el).text(name);
	}
	
	function lookupLocationName(apiKey, locationId) {
		$.getJSON(url);
	}

	$('button.wu-loc-search-open').on('click', function() {
		var container = wuContainer(this);
		
		// stash for when selection is made
		activeContainer = container;

		chooseBtn.data('params', container.data());
		$('#wu-location-lookup-modal').modal('show');
	});
	
	$('.wu-loc-id').text(function(i, el) {
		var me = this,
			container = wuContainer(this),
			apiKey = container.data().apikey,
			locId = container.data().lid,
			url ='http://api.wunderground.com/api/' +apiKey +'/geolookup' +locId +'.json';	
		if ( apiKey && locId ) {
			$.getJSON(url, function(json) {
				showLocationNameResult(json, me);
			});
		}
		return locId;
	});
});
