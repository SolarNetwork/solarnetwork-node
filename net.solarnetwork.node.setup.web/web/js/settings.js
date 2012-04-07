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
	var providerKey = params.provider;
	if ( params.instance !== undefined ) {
		providerKey += '.'+params.instance;
	}
	if ( updates[providerKey] === undefined ) {
		updates[providerKey] = {};
	}
	updates[providerKey][params.setting] = {domID: params.key, provider: params.provider, instance: params.instance, value: value};
	
	$('#'+params.key+'v').addClass('dirty');

	$('#submit').removeAttr('disabled');
};

/**
 * Setup a "info" dialog to display the full text of a setting.
 * 
 * @param params.title {String} the dialog title
 * @param params.key {String} the DOM ID with the dialog content
 */
SolarNode.Settings.addInfoDialog = function(params) {
	var dialog = undefined;
	var content = $('#'+params.key);
	$(content.clone())
		.insertBefore(content)
		.removeAttr('id')
		.removeClass()
		.addClass('description-brief')
		.click(function() {
			if ( dialog === undefined ) {
				dialog = $(content).dialog({
					autoOpen: false,
					title: params.title
				});
			}
			dialog.dialog('open');
			return false;
		})
		.before('<div class="dotdotdot">...</div>');
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
	var slider = $('#'+params.key);
	slider.slider({
		min: (params.min != '' ? Number(params.min) : 0),
		max: (params.max != '' ? Number(params.max) : 1),
		step: (params.step != '' ? Number(params.step) : 1),
		value: params.value,
		change: function(event, ui) {
				SolarNode.Settings.updateSetting(params, ui.value);
			},
		slide: function(event, ui) {
		      $(this).find('a:first').text(ui.value);
		    }
	}).after('<div class="slider-min caption">'+params.min+'</div>')
		.after('<div class="slider-max caption">'+params.max+'</div>');
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
	var toggle = $('#'+params.key).buttonset();
	var radios = $('input:radio[name='+params.key+']');
	radios.filter('[value='+params.value+']').attr('checked', 'checked');
	toggle.buttonset('refresh');
	radios.change(function() {
			var value = radios.filter(':checked').val();
			SolarNode.Settings.updateSetting(params, value);
		});
};


SolarNode.Settings.addTextField = function(params) {
	var field = $('#'+params.key);
	field.change(function() {
			var value = field.val();
			SolarNode.Settings.updateSetting(params, value);
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
			type: 'POST',
			url: url,
			data: formData,
			success: function(data, textStatus, xhr) {
				var providerKey = undefined, key = undefined, domID = undefined;
				if ( data.success === true && msg !== undefined && msg.success !== undefined ) {
					// update DOM with updated values
					for ( providerKey in updates ) {
						for ( key in updates[providerKey] ) {
							domID = updates[providerKey][key].domID;
							$('#'+domID+'v').text(updates[providerKey][key].value).removeClass('dirty');
						}
					}
					SolarNode.Settings.reset();
					$('<div>'+msg.success+'</div>').dialog({
						modal: true,
						title: msg.title,
						buttons: buttons
						});
				}
			},
			error: function(jqXHR, textStatus, errorThrown) {
				if ( msg !== undefined && msg.error !== undefined ) {
					$('<div>'+msg.error+'</div>').dialog({
						modal: true,
						title: msg.title,
						buttons: buttons
						});
				}
			},
			dataType: 'json'
		});
	}
	return false;
};

SolarNode.Settings.addFactoryConfiguration = function(params) {
	$.post(params.url, {uid: params.factoryUID}, function(data, textStatus) {
		window.location.reload(true);
	});
};

SolarNode.Settings.deleteFactoryConfiguration = function(params) {
	var buttons = {};
	buttons[params['delete.label']] = function() {
		$.post(params.url, {uid: params.factoryUID, instance: params.instanceUID}, function(data, textStatus) {
			window.location.reload(true);
		});
	};
	buttons[params['cancel.label']] = function() {
		$(this).dialog('close');
	};
	$('#alert-delete').dialog({
		title: params.title,
		height: 140,
		modal: true,
		buttons: buttons
	});
};
