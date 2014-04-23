SolarNode.Settings = {
		
};

SolarNode.Settings.runtime = {};
SolarNode.Settings.updates = {};

SolarNode.Settings.reset = function() {
	SolarNode.Settings.updates = {};
	$('#submit').attr('disabled', 'disabled');
};

/**
 * Cache the value for specific setting.
 * 
 * <p>This method is used to cache locally a changed setting value.</p>
 */
SolarNode.Settings.updateSetting = function(params, value) {
	//providerKey, key, domID, value) {
	var updates = SolarNode.Settings.updates;
	var instance = (params.instance !== undefined && params.instance !== '' ? params.instance : undefined);
	var providerKey = params.provider;
	if ( instance !== undefined ) {
		providerKey += '.'+instance;
	}
	if ( updates[providerKey] === undefined ) {
		updates[providerKey] = {};
	}
	updates[providerKey][params.setting] = {domID: params.key, 
			provider: params.provider, 
			instance: instance, 
			xint: params.xint === 'true' ? true : false,
			value: value};
	
	// show the "active value" element
	$('#cg-'+params.key+' span.active-value').removeClass('clean');

	$('#submit').removeAttr('disabled');
};

/**
 * Setup a new Slider control.
 * 
 * @param params.key {String} the DOM element ID for the slider
 * @param params.min {Number} the minimum value
 * @param params.max {Number} the maximum value
 * @param params.step {Number} the slider step value
 * @param params.value {Number} the initial value
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 */
SolarNode.Settings.addSlider = function(params) {
	var el = $('#'+params.key);
	if ( SolarNode.Settings.runtime.sliders === undefined ) {
		SolarNode.Settings.runtime.sliders = [];
	}
	var slider = new SolarNode.Class.Slider(el, {
		min: (params.min != '' ? Number(params.min) : 0),
		max: (params.max != '' ? Number(params.max) : 1),
		step: (params.step != '' ? Number(params.step) : 1),
		value: params.value,
		handleWidth: 42,
		showValue: true,
		change: function(event, ui) {
				SolarNode.Settings.updateSetting(params, ui.value);
			}
	});
	SolarNode.Settings.runtime.sliders.push(slider);
};

/**
 * Setup a new Toggle control.
 * 
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the slider
 * @param params.on {String} the "on" value
 * @param params.off {String} the "off" value
 * @param params.value {Number} the initial value
 */
SolarNode.Settings.addToggle = function(params) {	
	var toggle = $('#'+params.key);
	toggle.button();
	toggle.click(function() {
		toggle.button('toggle');
		var active = toggle.hasClass('active');
		var value = (active ? params.on : params.off);
		$(this).text(active ? params.onLabel : params.offLabel);
		if ( active ) {
			$(this).addClass('btn-success');
		} else {
			$(this).removeClass('btn-success');
		}
		SolarNode.Settings.updateSetting(params, value);
	});
};

/**
 * Setup a new radio control.
 * 
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the slider
 */
SolarNode.Settings.addRadio = function(params) {
	var radios = $('input:radio[name='+params.key+']');
	//radios.filter('[value='+params.value+']').attr('checked', 'checked');
	radios.change(function() {
			var value = radios.filter(':checked').val();
			SolarNode.Settings.updateSetting(params, value);
		});
};

/**
 * Setup a new text field.
 * 
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the slider
 * @param params.value {String} the initial value
 */
SolarNode.Settings.addTextField = function(params) {
	var field = $('#'+params.key);
	field.change(function() {
			var value = field.val();
			SolarNode.Settings.updateSetting(params, value);
		});
};

/**
 * Setup a new location finder field.
 * 
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the control
 * @param params.value {String} the initial value
 */
SolarNode.Settings.addLocationFinder = function(params) {
	var label = $('#'+params.key);
	var labelSpan = label.find('.setting-value');
	var btn = label.find('.btn');
	var lcType = params.locationType.toLowerCase();
	var modalRuntimeKey = lcType+'Modal';
	var modal = $('#'+lcType+'-lookup-modal');
	var chooseBtn = $('#'+lcType+'-lookup-choose');
	var selectedLocation = undefined;
	var tbody = modal.find('tbody');
	var searchBtn = modal.find('button[type=submit]');
	
	if ( SolarNode.Settings.runtime[modalRuntimeKey] === undefined ) {
		SolarNode.Settings.runtime[modalRuntimeKey] = modal.modal({show:false});
		modal.ajaxForm({
			dataType: 'json',
			beforeSubmit: function(dataArray, form, options) {
				// start a spinner on the search button so we know a search is happening
				searchBtn.attr('disabled', 'disabled');
				searchBtn.spin('small');
			},
			success: function(json, status, xhr, form) {
				// remove spinner and re-enable search button
				searchBtn.data('spinner').stop();
				searchBtn.removeAttr('disabled');
				if ( json.success !== true ) {
					SolarNode.errorAlert("Error querying SolarNetwork for locations: " +json.message);
					return;
				}
				var results = json.data.results;
				var i, len;
				var tr;
				var loc;
				tbody.empty();
				for ( i = 0, len = results.length; i < len; i++ ) {
					tr = $('<tr>');
					tr.data('location', results[i]);
					// common lookup
					$('<td>').text(results[i].sourceName).appendTo(tr);
					$('<td>').text(results[i].locationName).appendTo(tr);
					
					// price lookup
					if ( lcType === 'price' ) {
						$('<td>').text(results[i].currency).appendTo(tr);
					} else if ( lcType === 'weather' ) {
						loc = results[i].location;
						$('<td>').text(loc.country !== undefined ? loc.country : '').appendTo(tr);
						$('<td>').text(loc.postalCode !== undefined ? loc.postalCode : '').appendTo(tr);
					}
					tr.on('click', function() {
						var me = $(this);
						if ( me.hasClass('success') === false ) {
							me.parent().find('tr.success').removeClass('success');
							me.addClass('success');
						}
						selectedLocation = me.data('location');
						chooseBtn.removeAttr('disabled');
					});
					tbody.append(tr);
				}
				tbody.parent().removeClass('hidden');
			},
			error: function(xhr, status, statusText) {
				SolarNode.errorAlert("Error querying SolarNetwork for locations: " +statusText);
			}
		});
		modal.on('hidden', function() {
			chooseBtn.attr('disabled', 'disabled');
			chooseBtn.unbind();
			tbody.find('tr.success').removeClass('success');
		});
		modal.on('shown', function() {
			var firstInput = modal.find('input').first();
			firstInput.focus().select();
		});
	}
	btn.click(function() {
		// common lookup
		modal.find('input[name=sourceName]').val(params.sourceName);
		modal.find('input[name=locationName]').val(params.locationName);
		
		// price lookup
		modal.find('input[name=currency]').val(params.currency);
		
		// weather lookup
		modal.find('input[name="location.country"]').val(params.country);
		modal.find('input[name="location.postalCode"]').val(params.postalCode);
		
		chooseBtn.on('click', function() {
			if ( selectedLocation !== undefined ) {
				var msg = SolarNode.i18n(params.valueLabel, [selectedLocation.sourceName, selectedLocation.locationName]);
				labelSpan.text(msg);
				SolarNode.Settings.updateSetting(params, selectedLocation.id);
			}
			modal.modal('hide');
		});
		modal.modal('show');
	});
};

/**
 * Post any setting changes back to the node.
 * 
 * @param url {String} the URL to post to
 * @param msg.title {String} the result dialog title
 * @param msg.success {String} the message to display for a successful post
 * @param msg.error {String} the message to display for an error post
 */
SolarNode.Settings.saveUpdates = function(url, msg) {
	var updates = SolarNode.Settings.updates;
	var formData = '';
	var i = 0;
	var providerKey = undefined, key = undefined;
	for ( providerKey in updates ) {
		for ( key in updates[providerKey] ) {
			if ( formData.length > 0 ) {
				formData += '&';
			}
			formData += 'values[' +i+'].providerKey=' +encodeURIComponent(updates[providerKey][key].provider);
			if ( updates[providerKey][key].instance !== undefined ) {
				formData += '&values[' +i+'].instanceKey=' +encodeURIComponent(updates[providerKey][key].instance);
			}
			formData += '&values[' +i+'].transient=' +updates[providerKey][key].xint;
			formData += '&values[' +i+'].key=' +encodeURIComponent(key);
			formData += '&values[' +i+'].value=' +encodeURIComponent(updates[providerKey][key].value);
			i++;
		}
	}
	var buttons = {};
	buttons[msg.button] = function() {
		$(this).dialog('close');
	};
	if ( formData.length > 0 ) {
		$.ajax({
			type: 'post',
			url: url,
			data: formData,
			success: function(data, textStatus, xhr) {
				var providerKey = undefined, key = undefined, domID = undefined;
				if ( data.success === true && msg !== undefined && msg.success !== undefined ) {
					// update DOM with updated values
					for ( providerKey in updates ) {
						for ( key in updates[providerKey] ) {
							domID = updates[providerKey][key].domID;
							$('#cg-'+domID+' span.active-value').addClass('clean').find('.value').text(updates[providerKey][key].value);
						}
					}
					SolarNode.Settings.reset();
					$('<div class="alert alert-info fade in"><button class="close" data-dismiss="alert" type="button">×</button>'
							+'<strong>'+msg.title+':</strong> ' +msg.success +'</div>').insertBefore('#settings form div.actions');
				}
			},
			error: function(jqXHR, textStatus, errorThrown) {
				if ( msg !== undefined && msg.error !== undefined ) {
					$('<div class="alert alert-error fade in"><button class="close" data-dismiss="alert" type="button">×</button>'
							+'<strong>'+msg.title+':</strong> ' +msg.error +'</div>').insertBefore('#settings form div.actions');
				}
			},
			dataType: 'json'
		});
	}
	return false;
};

SolarNode.Settings.addFactoryConfiguration = function(params) {
	$(params.button).attr('disabled', 'disabled');
	$.post(params.url, {uid: params.factoryUID}, function(data, textStatus) {
		setTimeout(function() {
			window.location.reload(true);
		}, 500);
	});
};

/**
 * Show an alert element asking the user if they really want to delete
 * the selected factory configuration, and allow them to dismiss the alert
 * or confirm the deletion by clicking another button.
 */
SolarNode.Settings.deleteFactoryConfiguration = function(params) {
	var origButton = $(params.button);
	origButton.attr('disabled', 'disabled');
	var alert = $('#alert-delete').clone();
	
	var reallyDeleteButton = alert.find('button.submit');
	reallyDeleteButton.click(function() {
		$(this).attr('disabled', 'disabled');
		$.post(params.url, {uid: params.factoryUID, instance: params.instanceUID}, function(data, textStatus) {
			setTimeout(function() {
				window.location.reload(true);
			}, 500);
		});
	});
	alert.bind('close', function(e) {
		origButton.removeAttr('disabled');
		origButton.removeClass('hidden');
		reallyDeleteButton.unbind();
	});
	origButton.addClass('hidden');
	alert.insertAfter(origButton).removeClass('hidden');
};

$(document).ready(function() {
	$('.help-popover').popover();
	
	$('#backup-now-btn').click(function(event) {
		event.preventDefault();
		var l = Ladda.create(this);
		var url = $(this.form).attr('action');
		l.start();
		$.post(url, function(data) {
			l.stop();
		});
	});
});
