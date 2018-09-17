'use strict';

SolarNode.OpenWeatherMap = {};

$(function() {
	var modal = $('#owm-location-lookup-modal'),
		modalBody = modal.find('.modal-body'),
		form = modal.get(0),
		tbody = modal.find('tbody'),
		templateRow = modal.find('tr.template'),
		searchBtn = modal.find('button[type=submit]'),
		chooseBtn = modal.find('button.choose');
	
	var activeContainer;
	
	function offsetSettingId(settingId, offset) {
		var match = settingId.match(/^(.*i)(\d+)$/);
		return match[1] + String(Number(match[2]) - offset);
	}
	
	function timeZoneSettingTextField() {
		// this is tied directly to the settings order returned by ConfigurableOwmClientService, unfortunately 
		return $('#settings :text:eq(4)');
	}
	
	function activeApiKey() {
		var currParams = modal.data('params'),
			apiKeySettingId,
			apiKey;
		if ( currParams && currParams.settingId ) {
			apiKeySettingId = offsetSettingId(currParams.settingId, -3);
			
			return $('input#'+apiKeySettingId).val();
		}
		return '';
	}
	
	function showLocationSearchResults(json) {
		//searchBtn.removeAttr('disabled');
		if ( !Array.isArray(json.list) ) {
			SolarNode.error("Error querying OpenWeatherMap for locations: " +JSON.stringify(json), modalBody);
			return;
		}
		var results = json.list;
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

	function showLocationNameResult(json, el) {
		if ( !json || !json.sys ) {
			return;
		}
		var c = json.sys.country,
			lName = json.name,
			name = lName +', ' +c;
		$(el).text(name);
	}
	
	function handleAjaxError(xhr, settings, exception) {
		var apiKey = activeApiKey(),
			status = xhr.status,
			contentType = xhr.getResponseHeader('Content-Type'),
			json,
			message;
		if ( !apiKey || (status >= 400 && status < 500) ) {
			message = 'Invalid API key. Please see http://openweathermap.org/faq for more info.';
		} else if ( xhr.responseText && contentType !== undefined && contentType.toLowerCase().startsWith('application/json') ) {
			json = JSON.parse(xhr.responseText);
			message = json.message;
		} else {
			message = xhr.statusText;
		}
		SolarNode.error('Error querying OpenWeatherMap for locations: ' +message, modalBody);
		SolarNode.hideLoading(searchBtn);
	}
			
	// use JSONP to query
	modal.on('submit', function(event) {
		event.preventDefault();
		
		// start a spinner on the search button so we know a search is happening
		SolarNode.showLoading(searchBtn);
		
		// clear any previous selection
		chooseBtn.removeData('locationMeta');
		
		var apiKey = activeApiKey(),
			url = modal.attr('action') +'?q=' +encodeURIComponent(form.elements['query'].value)
				+'&units=metric'
				+'&appid=' +encodeURIComponent(apiKey);
		
		$.getJSON(url).done(function(json) {
			showLocationSearchResults(json);
			SolarNode.hideLoading(searchBtn);
		}).fail(handleAjaxError);
	});
	
	$('.owm-loc-lookup-modal button.choose').on('click', function() {
		var me = $(this),
			selectedLocation = me.data('locationMeta'),
			currParams = modal.data('params');
		if ( selectedLocation !== undefined ) {
			var locNameEl = activeContainer.find('.owm-loc-id'),
				locSettingId = offsetSettingId(currParams.settingId, -1);
			showLocationNameResult(selectedLocation, locNameEl);
			$('input#'+locSettingId).val(selectedLocation.id).trigger('change');
		}
		modal.modal('hide');
	});
	
	function owmContainer(el) {
		return $(el).parents('.setup-resource-container');
	}
	
	$('button.owm-loc-search-open').on('click', function() {
		var container = owmContainer(this);
		
		// stash for when selection is made
		activeContainer = container;
		
		modalBody.find('.alert').remove();
		modal.data('params', container.data()).modal('show');
	});
	
	$('.owm-loc-id').text(function(i, el) {
		var me = this,
			container = owmContainer(this),
			apiKey = container.data().apikey,
			locId = container.data().lid,
			url ='https://api.openweathermap.org/data/2.5/weather?id=' +encodeURIComponent(locId)
				+'&units=metric'
				+'&appid=' +encodeURIComponent(apiKey);
		if ( apiKey && locId ) {
			$.getJSON(url, function(json) {
				showLocationNameResult(json, me);
			});
		}
		return locId;
	});
	
	// hook into location change, to pick up time zone population
	$('.sn-loc-lookup-modal button.choose').on('click', function() {
		var me = $(this);
		var l = me.data('locationMeta');
		var tzTextField = timeZoneSettingTextField();
		if ( tzTextField && l && l.location && l.location.timeZoneId ) {
			tzTextField.val(l.location.timeZoneId).trigger('change');
		}
	});
});
