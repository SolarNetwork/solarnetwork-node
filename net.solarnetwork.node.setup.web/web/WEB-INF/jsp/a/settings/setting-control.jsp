<%--
	Expects the following request-or-higher properties:
	
	settingsService - the SettingService
	provider - the current provider
	setting - the current setting
	settingId - the ID to use for the setting input
	instanceId - the instance ID to use
--%>
<c:choose>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.KeyedSettingSpecifier')}">
		<div class="control-group" id="cg-${settingId}">
			<label class="control-label" for="${settingId}">
				<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}" text="${setting.key}"/>
			</label>
			<div class="controls">
				<c:choose>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.SliderSettingSpecifier')}">
						<div id="${settingId}" class="setting slider"></div>
						<script>
						$(function() {
							SolarNode.Settings.addSlider({
								key: '${settingId}',
								min: '${setting.minimumValue}',
								max: '${setting.maximumValue}',
								step: '${setting.step}',
								value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.ToggleSettingSpecifier')}">
						<div id="${settingId}" class="setting toggle">
							<input type="radio" name="${settingId}" id="${settingId}t" value="${setting.trueValue}" />
								<label for="${settingId}t"><fmt:message key="settings.toggle.on"/></label>
							<input type="radio" name="${settingId}" id="${settingId}f" value="${setting.falseValue}" />
								<label for="${settingId}f"><fmt:message key="settings.toggle.off"/></label>
						</div>
						<script>
						$(function() {
							SolarNode.Settings.addToggle({
								key: '${settingId}',
								on: '${setting.trueValue}',
								off: '${setting.falseValue}',
								value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TextFieldSettingSpecifier')}">
						<input type="text" name="${settingId}" id="${settingId}" class="span5" maxLength="255"
							value="<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>" />
						<script>
						$(function() {
							SolarNode.Settings.addTextField({
								key: '${settingId}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TitleSettingSpecifier')}">
						<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
					</c:when>
				</c:choose>
				
				<c:set var="help">
					<setup:message key='${setting.key}.desc' messageSource='${provider.messageSource}'/>
				</c:set>
				<button type="button" class=" help-popover help-icon" tabindex="-1"
						data-content="${fn:escapeXml(help)}"
						data-html="true">
					<i class="icon-question-sign"></i>
				</button>

				<span class="help-inline active-value clean"><span class="text-info">
					<fmt:message key="settings.current.value.label"/>
					<span class="value">
						<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
					</span>
				</span></span>
				
			</div>
		</div>
	</c:when>
</c:choose>
