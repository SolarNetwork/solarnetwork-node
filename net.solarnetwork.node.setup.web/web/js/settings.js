(function() {
'use strict';

SolarNode.Settings = {

	/** A regular expression that matches a cron expression. */
	CRON_REGEX : /([0-9*?\/,-]+)\s+([0-9*?\/,-]+)\s+([0-9*?\/,-]+)\s+([0-9*?\/,-]+)\s+([0-9*?\/,-]+)\s+([0-9*?\/,-]+)(.*)/,

	/** A regular expression that matches a cron slash-based period. */
	CRON_FIELD_PERIOD_REGEX : /^([0-9]+|\*)\/([0-9]+|\*)$/,

};

SolarNode.Settings.runtime = {};
SolarNode.Settings.updates = {};

function delayedReload() {
	setTimeout(function() {
		window.location.reload(true);
	}, 500);
}

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
 * @param params.key {String} the DOM element ID for the button
 * @param params.on {String} the "on" value
 * @param params.off {String} the "off" value
 * @param params.value {Number} the initial value
 */
SolarNode.Settings.addToggle = function(params) {
	const toggle = $('#'+params.key);
	toggle.button();
	toggle.on('click', function() {
		toggle.button('toggle');
		const active = toggle.hasClass('active');
		const value = (active ? params.on : params.off);
		toggle.text(active ? params.onLabel : params.offLabel);
		if ( active ) {
			toggle.addClass('btn-success').removeClass('btn-light')
		} else {
			toggle.removeClass('btn-success').addClass('btn-light');
		}
		SolarNode.Settings.updateSetting(params, value);
	});
};

/**
 * Setup a new radio control.
 *
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the radio buttons
 */
SolarNode.Settings.addRadio = function(params) {
	var radios = $('input:radio[name='+params.key+']');
	//radios.filter('[value='+params.value+']').attr('checked', 'checked');
	radios.on('change', function() {
			var value = radios.filter(':checked').val();
			SolarNode.Settings.updateSetting(params, value);
		});
};

/**
 * Setup a new select menu.
 *
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the select element
 */
SolarNode.Settings.addSelect = function(params) {
	var select = $('select[name='+params.key+']');
	select.on('change', function() {
			var value = select.val();
			SolarNode.Settings.updateSetting(params, value);
		});
};

/**
 * Setup a new text field.
 *
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the text field
 */
SolarNode.Settings.addTextField = function(params) {
	var field = $('#'+params.key);
	field.on('change', function() {
			var value = field.val();
			SolarNode.Settings.updateSetting(params, value);
		});
};

/**
 * Setup a new schedule field.
 *
 * @param params.provider {String} the provider key
 * @param params.setting {String} the setting key
 * @param params.key {String} the DOM element ID for the text field
 */
SolarNode.Settings.addScheduleField = function(params) {
	var field = $('#'+params.key);
	var select = $('#cg-'+params.key).find('select');
	var prevOption = select.val();
	var prevCronMatch = field.val().match(SolarNode.Settings.CRON_REGEX);

	(function() {
		// reset the initial form fields to friendly values if possible
		var expr = field.val();
		var exprNum = Number(expr);
		var resultOption = 'cron';
		var resultExpr = expr;
		if ( !isNaN(exprNum) ) {
			// it is a number, so translate into largest possible unit
			if ( exprNum >= 3600000 && exprNum % 3600000 === 0 ) {
				resultOption = 'h';
				resultExpr = exprNum / 3600000;
			} else if ( exprNum >= 60000 && exprNum % 60000 === 0 ) {
				resultOption = 'm';
				resultExpr = exprNum / 60000;
			} else if ( exprNum >= 1000 && exprNum % 1000 === 0 ) {
				resultOption = 's';
				resultExpr = exprNum / 1000;
			} else {
				resultOption = 'ms';
				resultExpr = exprNum;
			}
		}
		if ( resultOption !== prevOption ) {
			select.val(resultOption);
			field.val(resultExpr);
			prevOption = resultOption;
			prevCronMatch = null;
		}
	})();

	function valueForSetting(option, value) {
		var valueNum = Number(value);
		if ( option === 's' && !isNaN(valueNum) ) {
			value = value * 1000;
		} else if ( option === 'm' && !isNaN(valueNum) ) {
			value = value * 60000;
		} else if ( option === 'h' && !isNaN(valueNum) ) {
			value = value * 3600000;
		}
		return String(value);
	}

	field.on('change', function() {
		var value = valueForSetting(prevOption, field.val());
		var cronMatch = value.match(SolarNode.Settings.CRON_REGEX);
		if ( cronMatch ) {
			if ( prevOption !== 'cron' ) {
				select.val('cron');
				prevOption = 'cron';
			}
			prevCronMatch = cronMatch;
		} else if ( prevOption === 'cron' ) {
			select.val('ms');
			prevOption = 'ms';
			prevCronMatch = null;
		}
		SolarNode.Settings.updateSetting(params, value);
	});

	select.on('change', function() {
		var option = select.val();
		var expr = field.val();
		var exprNum = Number(expr);
		var resultExpr = expr;
		var settingValue;

		function updateResultForCronFieldPeriod(cronMatch, cronFieldIndex, multiplier, base) {
			if ( cronMatch ) {
				const fieldMatch = cronMatch[cronFieldIndex].match(SolarNode.Settings.CRON_FIELD_PERIOD_REGEX);
				if ( fieldMatch ) {
					resultExpr = fieldMatch[2] === '*' ? multiplier : Number(fieldMatch[2]) * multiplier;
				} else {
					resultExpr = (multiplier > 1 ? base : multiplier);
				}
			} else {
				resultExpr = multiplier;
			}
		}

		if ( option === 'cron' ) {
			if ( !isNaN(exprNum) ) {
				if ( prevOption === 'ms' && exprNum <= 60000 ) {
					resultExpr = '0/' + Math.ceil(exprNum / 1000) + ' * * * * *';
				} else if ( prevOption === 's' && exprNum <= 6000 ) {
					resultExpr = '0/' + Math.ceil(exprNum) + ' * * * * *';
				} else if ( prevOption === 'm' && exprNum <= 60 ) {
					resultExpr = '0 0/' + Math.ceil(exprNum) + ' * * * *';
				} else if ( prevOption === 'h' && exprNum <= 24 ) {
					resultExpr = '0 0 0/' + Math.ceil(exprNum) + ' * * *';
				} else {
					resultExpr = '0 * * * * *';
				}
			} else {
				resultExpr = '0 * * * * *';
			}
		} else if ( prevOption === 'cron' ) {
			if ( option === 'ms' ) {
				updateResultForCronFieldPeriod(prevCronMatch, 1, 1000, 60000);
			} else if ( option === 's' ) {
				updateResultForCronFieldPeriod(prevCronMatch, 1, 1, 60);
			} else if ( option === 'm' ) {
				updateResultForCronFieldPeriod(prevCronMatch, 2, 1, 60);
			} else if ( option === 'h' ) {
				updateResultForCronFieldPeriod(prevCronMatch, 3, 1, 24);
			} else {
				resultExpr = 1;
			}
		} else if ( prevOption === 'ms' ) {
			if ( isNaN(exprNum) ) {
				resultExpr = '500';
			} else if ( option === 's' ) {
				resultExpr /= 1000;
			} else if ( option === 'm' )  {
				resultExpr /= 60000;
			} else if ( option === 'h' ) {
				resultExpr /= 3600000;
			}
		} else if ( prevOption === 's' ) {
			if ( isNaN(exprNum) ) {
				resultExpr = '30';
			} else if ( option === 'ms' ) {
				resultExpr *= 1000;
			} else if ( option === 'm' )  {
				resultExpr /= 60;
			} else if ( option === 'h' ) {
				resultExpr /= 3600;
			}
		} else if ( prevOption === 'm' ) {
			if ( isNaN(exprNum) ) {
				resultExpr = '10';
			} else if ( option === 'ms' ) {
				resultExpr *= 60000;
			} else if ( option === 's' )  {
				resultExpr *= 60;
			} else if ( option === 'h' ) {
				resultExpr /= 60;
			}
		} else if ( prevOption === 'h' ) {
			if ( isNaN(exprNum) ) {
				resultExpr = '2';
			} else if ( option === 'ms' ) {
				resultExpr *= 3600000;
			} else if ( option === 's' )  {
				resultExpr *= 60000;
			} else if ( option === 'm' ) {
				resultExpr *= 60;
			}
		}
		if ( typeof resultExpr === 'number' ) {
			resultExpr = Math.ceil(resultExpr);
		}
		resultExpr = String(resultExpr);
		if ( resultExpr !== expr ) {
			field.val(resultExpr);
			settingValue = valueForSetting(option, resultExpr);
			if ( valueForSetting(prevOption, expr) !== settingValue ) {
				SolarNode.Settings.updateSetting(params, settingValue);
			}
		}
		prevOption = option;
		prevCronMatch = resultExpr.match(SolarNode.Settings.CRON_REGEX);
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
	var modal = $('.'+lcType+'-lookup-modal');
	var chooseBtn = modal.find('.choose');
	var tbody = modal.find('tbody');
	var templateRow = modal.find('tr.template');
	var searchBtn = modal.find('button[type=submit]');

	if ( SolarNode.Settings.runtime[modalRuntimeKey] === undefined ) {
		SolarNode.Settings.runtime[modalRuntimeKey] = modal.modal({show:false});
		modal.ajaxForm({
			dataType: 'json',
			beforeSubmit: function(dataArray, form, options) {
				// start a spinner on the search button so we know a search is happening
				SolarNode.showLoading(searchBtn);
				chooseBtn.prop('disabled', true);
				chooseBtn.removeData('locationMeta'); // clear any previous selection
				//searchBtn.attr('disabled', 'disabled');
			},
			success: function(json, status, xhr, form) {
				//searchBtn.removeAttr('disabled');
				if ( json.success !== true ) {
					SolarNode.errorAlert("Error querying SolarNetwork for locations: " +json.message);
					return;
				}
				var results = json.data.results;
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
						if ( prop === 'location.region' ) {
							// special case for Region to also show State if available
							var state = SolarNode.extractJSONPath(meta, 'location.stateOrProvince');
							if ( !val ) {
								// no region value available, so use state instead
								val = state;
							} else if ( state && state !== val ) {
								// both region and state available and not the same, so use "region, state"
								val = val +', ' +state;
							}
						}
						if ( val ) {
							td.text(val);
						}
					});

					tbody.append(tr);
				}
				tbody.parent().removeClass('hidden');
			},
			error: function(xhr, status, statusText) {
				SolarNode.errorAlert("Error querying SolarNetwork for locations: " +statusText);
			},
			complete: function() {
				//searchBtn.removeAttr('disabled', 'disabled');
				SolarNode.hideLoading(searchBtn);
			}
		});
	}
	btn.on('click', function() {
		// associate data with singleton modal
		chooseBtn.data('params', params);
		chooseBtn.data('label', labelSpan);
		modal.modal('show');
	});
};

SolarNode.Settings.addGroupedSetting = function(params) {
	var groupCount = $('#'+params.key),
		count = Number(groupCount.val()),
		container = groupCount.parent(),
		url = $(groupCount.get(0).form).attr('action');

	// wire up the Add button to add dynamic elements
	container.find('button.group-item-add').on('click', function() {
		var newCount = count + 1;
		container.find('button').attr('disabled', 'disabled');
		SolarNode.Settings.updateSetting(params, newCount);
		SolarNode.Settings.saveUpdates(url, undefined, delayedReload);
	});
	// dynamic grouped items remove support
	container.find('.group-item-remove').on('click', function() {
		if ( count < 1 ) {
			return;
		}
		var newCount = count - 1;
		container.find('button').attr('disabled', 'disabled');
		SolarNode.Settings.updateSetting(params, newCount);
		SolarNode.Settings.saveUpdates(url, undefined, delayedReload, {
			'settingKeyPrefixToClean' : params.indexed +'['+newCount+']'
		});
	}).each(function() {
		if ( count < 1 ) {
			$(this).attr('disabled', 'disabled');
		}
	});
};

SolarNode.Settings.addTextArea = function(params) {
	// TODO
};

SolarNode.Settings.addFile = function(params) {
	// TODO
};

/**
 * Post any setting changes back to the node.
 *
 * @param url {String} the URL to post to
 * @param msg.title {String} the result dialog title
 * @param msg.success {String} the message to display for a successful post
 * @param msg.error {String} the message to display for an error post
 * @param resultCallback {Function} optional callback to invoke after updates saved, passed error as parameter
 * @param extraFormData {Object} any extra form data to include in the submission
 */
SolarNode.Settings.saveUpdates = function(url, msg, resultCallback, extraFormData) {
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
	if ( extraFormData ) {
		for ( key in extraFormData ) {
			if ( formData.length > 0 ) {
				formData += '&';
			}
			formData += encodeURIComponent(key) + '=' + encodeURIComponent(extraFormData[key]);
		}
	}
	var buttons = {};
	if ( msg && msg.button ) {
		buttons[msg.button] = function() {
			$(this).dialog('close');
		};
	}
	if ( formData.length > 0 ) {
		$.ajax({
			type: 'post',
			url: url,
			data: formData,
	    	beforeSend: function(xhr) {
	    		SolarNode.csrf(xhr);
	    	},
			success: function(data, textStatus, xhr) {
				var providerKey = undefined, key = undefined, domID = undefined;
				if ( resultCallback ) {
					resultCallback();
				} else if ( data.success === true && msg !== undefined && msg.success !== undefined ) {
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
				if ( resultCallback ) {
					resultCallback(textStatus);
				} else if ( msg !== undefined && msg.error !== undefined ) {
					$('<div class="alert alert-error fade in"><button class="close" data-dismiss="alert" type="button">×</button>'
							+'<strong>'+msg.title+':</strong> ' +msg.error +'</div>').insertBefore('#settings form div.actions');
				}
			},
			dataType: 'json'
		});
	}
	return false;
};

/**
 * Show an alert element asking the user if they really want to delete
 * the selected factory configuration, and allow them to dismiss the alert
 * or confirm the deletion by clicking another button.
 */
SolarNode.Settings.deleteFactoryConfiguration = function(params) {
	params['alert'] = '#alert-delete'
	SolarNode.Settings.showConfirmation(params);
};

/**
 * Show an alert element asking the user if they really want to reset
 * the selected factory configuration to default, and allow them to dismiss the alert
 * or confirm the reset by clicking another button.
 */
SolarNode.Settings.resetFactoryConfiguration = function(params) {
	params['alert'] = '#alert-reset'
	SolarNode.Settings.showConfirmation(params);
};

/**
 * Show an alert element asking the user if they if they want to complete
 * the current action
 */
SolarNode.Settings.showConfirmation = function(params) {
	var origButton = $(params.button);
	origButton.attr('disabled', 'disabled');
	var alert = $(params.alert).clone();

	var confirmationButton = alert.find('button.submit');
	confirmationButton.on('click', function() {
		$(this).attr('disabled', 'disabled');
		$.ajax({
			type : 'POST',
			url : params.url,
			data : {uid: params.factoryUid, instance: params.instanceUid},
			beforeSend: function(xhr) {
				SolarNode.csrf(xhr);
	        },
			success: delayedReload
		});
	});
	alert.on('close.bs.alert', function(e) {
		origButton.removeAttr('disabled');
		origButton.removeClass('hidden');
		confirmationButton.unbind();
	});
	origButton.addClass('hidden');
	alert.insertAfter(origButton).removeClass('hidden');
};


function formatTimestamp(date) {
	if ( !date ) {
		return;
	}
	return moment(date).format('D MMM YYYY HH:mm');
}

function refreshBackupList() {
	$.getJSON(SolarNode.context.path('/a/backups'), function(json) {
		if ( json.success !== true ) {
			SolarNode.error(json.message, $('#backup-list-form'));
			return;
		}
		var optionEl = $('#backup-backups').get(0);
		while ( optionEl.length > 0 ) {
			optionEl.remove(0);
		}
		if ( Array.isArray(json.data) ) {
			json.data.forEach(function(backup) {
				var date = new Date(backup.date),
					nodeId = backup.nodeId;
				optionEl.add(new Option('Node ' +nodeId + ' @ ' +formatTimestamp(date), backup.key));
			});
		}
		optionEl.selectedIndex = 0;
	});
}

function setupBackups() {
	var createBackupSubmitButton = $('#backup-now-btn');

	$('#create-backup-form').ajaxForm({
		dataType : 'json',

		beforeSubmit : function(dataArray, form, options) {
			SolarNode.showSpinner(createBackupSubmitButton);
			createBackupSubmitButton.attr('disabled', 'disabled');
		},
		success : function(json, status, xhr, form) {
			if ( json.success !== true || json.data === undefined || json.data.key === undefined ) {
				SolarNode.errorAlert("Error creating backup: " +json.message);
				return;
			}
			refreshBackupList();
		},
		error : function(xhr, status, statusText) {
			SolarNode.errorAlert("Error creating new backup: " +statusText);
		},
		complete : function() {
			createBackupSubmitButton.removeAttr('disabled');
			SolarNode.hideSpinner(createBackupSubmitButton);
		}
	});

	$('#backup-restore-button').on('click', function(event) {
		var form = event.target.form,
			backupKey = form.elements['backup-backups'].value;
		$.getJSON(SolarNode.context.path('/a/backups/inspect')+'?key='+encodeURIComponent(backupKey), function(json) {
			if ( json.success !== true ) {
				SolarNode.error(json.message, $('#backup-list-form'));
				return;
			}
			SolarNode.Backups.generateBackupList(json.data, $('#backup-restore-list-container'));
			var form = $('#backup-restore-modal');
			form.find('input[name=key]').val(backupKey);
			form.modal('show');
		});
	});

	$('#backup-restore-list-container').on('click', 'div.menu-item', function(event) {
		var row = $(this),
			selectedCount = 0,
			submit = $('#backup-restore-modal button[type=submit]');
		row.toggleClass('selected');
		selectedCount = row.parent().children('.selected').length;
		if ( selectedCount < 1 ) {
			submit.attr('disabled', 'disabled');
		} else {
			submit.removeAttr('disabled');
		}
	});

	$('#backup-restore-modal').ajaxForm({
		dataType : 'json',
		beforeSubmit : function(dataArray, form, options) {
			var providers = SolarNode.Backups.selectedProviders($('#backup-restore-list-container')),
				form = $('#backup-restore-modal'),
				submitBtn = form.find('button[type=submit]');
			Array.prototype.splice.apply(dataArray, [dataArray.length, 0].concat(providers));
			submitBtn.attr('disabled', 'disabled');
			SolarNode.showSpinner(submitBtn);
		},
		success : function(json, status, xhr, form) {
			if ( json.success !== true ) {
				SolarNode.error(json.message, $('#backup-restore-modal div.modal-body'));
				return;
			}
			var form = $('#backup-restore-modal');
			SolarNode.info(json.message, $('#backup-restore-list-container').empty());
			form.find('button, .modal-body p').remove();
			form.find('.progress.hidden').removeClass('hidden');
			setTimeout(function() {
				SolarNode.tryGotoURL(SolarNode.context.path('/a/settings'));
			}, 10000);
		},
		error : function(xhr, status, statusText) {
			SolarNode.error("Error restoring backup: " +statusText, $('#backup-restore-modal div.modal-body'));
		},
		complete : function() {
			createBackupSubmitButton.removeAttr('disabled');
			SolarNode.hideSpinner(createBackupSubmitButton);
		}
	}).on('show.bs.modal', function() {
		$(this).find('button[type=submit]').removeAttr('disabled');
	});
}

function uploadSettingResourceProgress(event) {
	if ( event.lengthComputable ) {
		var percentComplete = event.loaded / event.total;
		console.info("Upload progress: %d%%", percentComplete * 100);
		SolarNode.GlobalProgress.update(percentComplete);
	}
}

function uploadSettingResourceDone(event) {
	var xhr = this;
	if ( xhr.status >= 200 && xhr.status < 300 ) {
		SolarNode.GlobalProgress.hide();
	} else {
		console.error("Error uploading setting resource (%d): %s", xhr.status, xhr.responseText);
	}
}

function uploadSettingResourceError(event) {
	var xhr = this;
	SolarNode.GlobalProgress.hide();
	console.error( "Error submitting image (%s), got response: %s", xhr.statusText, xhr.responseText);
}

function uploadSettingResource(url, provider, instance, setting, dataKey, dataValue) {
	SolarNode.GlobalProgress.show();
	var xhr = xhr = new XMLHttpRequest();
	var form = new FormData();
	var i;
	form.append("handlerKey", provider);
	if ( instance ) {
		form.append("instanceKey", instance);
	}
	form.append("key", setting);
	if ( dataValue instanceof FileList ) {
		for ( i = 0; i < dataValue.length; i++ ) {
			form.append(dataKey, dataValue[i]);
		}
	} else {
		form.append(dataKey, dataValue);
	}
	xhr.onload = uploadSettingResourceDone;
	xhr.onerror = uploadSettingResourceError;
	xhr.upload.addEventListener("progress", uploadSettingResourceProgress);
	xhr.open("POST", url);
	xhr.setRequestHeader("Accept", "application/json");
	SolarNode.csrf(xhr);
	xhr.send(form);
}

function setupComponentSettings(container) {
	container.find('.help-popover').each(function(i, el) {
		new bootstrap.Popover(el);
	});

	container.find('.setting-resource-upload').on('click', function() {
		var me = $(this);
		var id = me.data('key'),
			field = $('#'+id),
			url = me.data('action'),
			provider = me.data('provider'),
			instance = me.data('instance'),
			setting = me.data('setting');
		if ( field.length < 1 ) {
			return;
		}
		var el = field.get(0),
			val;
		if ( el.files ) {
			// input[type=file]
			if ( el.files.length > 0 ) {
				uploadSettingResource(url, provider, instance, setting, "file", el.files);
			}
		} else {
			// textarea
			val = field.val();
			if ( val.length > 0 ) {
				uploadSettingResource(url, provider, instance, setting, "data", val);
			}
		}
	});
	container.find('.settings-resource-export').on('click', function(event) {
		event.preventDefault();
		const target = this.dataset.target;
		const action = this.dataset.action;
		const select = document.querySelector(target);
		if ( select ) {
			const identOpt = select.selectedOptions[0];
			if ( identOpt ) {
				var query = 'handlerKey='+ encodeURIComponent(identOpt.dataset['handler'])
					+ '&key=' + encodeURIComponent(identOpt.dataset['key']);
				document.location = action +'?'+query;
			}
		}
		return false;
	});
	
	// copy buttons
	container.find('button.copy').on('click', function(event) {
		event.preventDefault();
		let copySrc = this.previousElementSibling;
		let copyEl = undefined;
		if ( copySrc.tagName === 'INPUT' ) {
			if ( copySrc.getAttribute('type') != 'password') {
				copyEl = $(copySrc);
			}
		} else {
			copyEl = $(copySrc);
		}
		if ( copyEl ) {
			if ( SolarNode.copyElementValue(copyEl) ) {
				const icon = $(this).find('i');
				icon.addClass('bi bi-clipboard2-check').removeClass('bi bi-clipboard2');
				setTimeout(() => {
					icon.addClass('bi bi-clipboard2').removeClass('bi bi-clipboard2-check');
				}, 1200);
			}
		}
	});
}

// instance carousel support

function loadComponentInstanceContainer(container) {
	if ( !(container) ) {
		return;
	}
	if ( container && container.length > 0 && !container.hasClass('loaded') ) {
		var url = container.data('bsTarget')
			+'?uid=' + encodeURIComponent(container.data('factoryUid'))
			+'&key=' + encodeURIComponent(container.data('instanceKey'));
		console.log('Loading component instance: ' +url);
		container.addClass('loaded');
		container.load(url, function() {
			setupComponentSettings(container);
			$('body').trigger('sn.settings.component.loaded', [container]);
		});
	}
}

function loadComponentInstance(instanceKey) {
	if ( !instanceKey ) {
		return;
	}
	var container = $('.instance-content[data-instance-key="'+instanceKey+'"]');
	loadComponentInstanceContainer(container);
}

function selectInitialComponentInstance() {
	var selected = false;
	var componentContent = $('.instance-content');
	
	if ( componentContent.length > 1 && document.location.hash ) {
		var instanceKey = decodeURIComponent(document.location.hash.substring(1));
		var instanceTab = $('#settings.carousel .page-indicators button[data-instance-key="'
			+ instanceKey + '"]');
		if ( instanceTab.length > 0 ) {
			instanceTab.first().trigger('click');
			selected = true;
		}
	}
	if ( !selected && componentContent.length > 0 ) {
		loadComponentInstanceContainer(componentContent.first());
	}
}
	
$(document).ready(function() {
	setupComponentSettings($('body'));
	
	$('.lookup-modal table.search-results tbody').on('click', 'tr', function() {
		var me = $(this);
		var form = me.closest('form');
		var chooseBtn = form.find('button.choose');
		if ( me.hasClass('table-success') === false ) {
			me.parent().find('tr.table-success').removeClass('table-success');
			me.addClass('table-success');
		}
		chooseBtn.data('locationMeta', me.data('locationMeta'));
		chooseBtn.removeAttr('disabled');
	});

	$('.lookup-modal').on('hidden.bs.modal', function() {
		var form = $(this);
		var chooseBtn = form.find('button.choose');
		chooseBtn.attr('disabled', 'disabled');
		chooseBtn.removeData('params');
		chooseBtn.removeData('label');
		form.find('table.search-results tr.table-success').removeClass('table-success');
	});
	$('.lookup-modal').on('shown.bs.modal', function() {
		var firstInput = $(this).find('input').first();
		firstInput.trigger('focus').trigger('select');
	});
	$('.sn-loc-lookup-modal button.choose').on('click', function() {
		var me = $(this);
		var modal = me.closest('.modal');
		var selectedLocation = me.data('locationMeta');
		var currParams = me.data('params');
		var nameSpan = me.data('label');
		if ( selectedLocation !== undefined ) {
			var msg = SolarNode.i18n(currParams.valueLabel, [SolarNode.extractJSONPath(selectedLocation, 'm.name'),
			                                                 SolarNode.extractJSONPath(selectedLocation, 'sourceId')]);
			nameSpan.text(msg);
			SolarNode.Settings.updateSetting(currParams, selectedLocation.locationId + ':' +selectedLocation.sourceId);
		}
		modal.modal('hide');
	});
	
	$('#add-component-instance-modal').ajaxForm({
		dataType: 'json',
		beforeSubmit: function(formData, jqForm, options) {
			$('#add-component-instance-modal').find('button[type=submit]').attr('disabled', 'disabled');
			return true;
		},
		success: function(json, status, xhr, form) {
			if ( json && json.success === true ) {
				if ( json.data ) {
					document.location.hash = encodeURIComponent(json.data);
				}
				delayedReload();
			} else {
				SolarNode.error(json.message, $('#add-component-instance-modal .modal-body.start'));
			}
		},
		error: function(xhr, status, statusText) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, $('#add-component-instance-modal .modal-body.start'));
		}
	}).on('shown.bs.modal', function() {
		$('#add-component-instance-name').val('').focus();
	});
	$('#remove-all-component-instance-modal').ajaxForm({
		dataType: 'json',
		beforeSubmit: function(formData, jqForm, options) {
			$('#remove-all-component-instance-modal').find('button[type=submit]').attr('disabled', 'disabled');
			return true;
		},
		success: function(json, status, xhr, form) {
			if ( json && json.success === true ) {
				delayedReload();
			} else {
				SolarNode.error(json.message, $('#remove-all-component-instance-modal .modal-body.start'));
			}
		},
		error: function(xhr, status, statusText) {
			var json = $.parseJSON(xhr.responseText);
			SolarNode.error(json.message, $('#remove-all-component-instance-modal .modal-body.start'));
		}
	});
	$('.delete-factory-instance').on('click', function() {
		var button = this;
		SolarNode.Settings.deleteFactoryConfiguration({
			button: button,
			url: button.dataset.bsTarget,
			factoryUid: button.dataset.factoryUid,
			instanceUid: button.dataset.instanceKey
		});
	});
	$('.reset-factory-instance').on('click', function() {
		var button = this;
		SolarNode.Settings.resetFactoryConfiguration({
			button: button,
			url: button.dataset.bsTarget,
			factoryUid: button.dataset.factoryUid,
			instanceUid: button.dataset.instanceKey
		});
	});
	
	$('#settings.carousel .page-indicators button').on('click', function(event) {
		var instanceKey = this.dataset.instanceKey;
		if ( !instanceKey ) {
			return;
		}
		console.log('Carousel click to instance: ' +instanceKey);
		if ( !this.classList.contains('btn-warning') ) {
			$('#settings.carousel .page-indicators button.btn-warning').removeClass('btn-warning').addClass('btn-secondary');
			this.classList.add('btn-warning');
			this.classList.remove('btn-secondary');
		}
		document.location.hash = encodeURIComponent(instanceKey);
		loadComponentInstance(instanceKey);
	});
	
	selectInitialComponentInstance();
	setupBackups();
});

}());
