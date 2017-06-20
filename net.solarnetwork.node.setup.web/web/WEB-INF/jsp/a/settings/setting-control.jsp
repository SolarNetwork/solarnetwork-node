<%--
	Expects the following request-or-higher properties:
	
	settingsService - the SettingService
	provider - the current provider
	setting - the current setting
	settingId - the ID to use for the setting input
	instanceId - the instance ID to use
	groupSetting - an optional group setting
	groupSettingId - an optional group setting ID
	groupIndex - an optional group index
--%>
<c:set var="settingValue" scope="page">
	<setup:settingValue service='${settingsService}' provider='${provider}' setting='${setting}'/>
</c:set>
<c:choose>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.SetupResourceSettingSpecifier')}">
		<setup:resources role="USER" type="text/html" inline="true"
			provider="${setting.setupResourceProvider}" 
			properties="${setting.setupResourceProperties}"
			wrapperElement="div"
			wrapperClass="control-group setup-resource-container"
			id="cg-${settingId}" 
			data-provider-id="${provider.settingUID}"
			data-setting-id="${settingId}"
			data-instance-id="${instanceId}"
			data-group-index="${groupIndex}"
			/>
	</c:when>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.KeyedSettingSpecifier')}">
		<div class="control-group" id="cg-${settingId}">
			<label class="control-label" for="${settingId}">
				<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}" text="${setting.key}" index="${groupIndex}"/>
			</label>
			<div class="controls ${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TitleSettingSpecifier') ? 'static' : ''}">
				<c:choose>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.SliderSettingSpecifier')}">
						<div id="${settingId}" class="setting slider span5"></div>
						<script>
						$(function() {
							SolarNode.Settings.addSlider({
								key: '${settingId}',
								min: '${setting.minimumValue}',
								max: '${setting.maximumValue}',
								step: '${setting.step}',
								value: '${fn:escapeXml(settingValue)}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.ToggleSettingSpecifier')}">
					    <button type="button" class="toggle btn<c:if test='${settingValue eq  setting.trueValue}'> btn-success active</c:if>" 
					    	id="${settingId}">
					    	<c:choose>
					    		<c:when test="${settingValue eq  setting.trueValue}">
					    			<fmt:message key="settings.toggle.on"/>
					    		</c:when>
					    		<c:otherwise>
					    			<fmt:message key="settings.toggle.off"/>
					    		</c:otherwise>
					    	</c:choose>
					    </button>
						<script>
						$(function() {
							SolarNode.Settings.addToggle({
								key: '${settingId}',
								on: '${setting.trueValue}',
								onLabel: '<fmt:message key="settings.toggle.on"/>',
								off: '${setting.falseValue}',
								offLabel: '<fmt:message key="settings.toggle.off"/>',
								value: '${fn:escapeXml(settingValue)}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.RadioGroupSettingSpecifier')}">
						<c:forEach items="${setting.valueTitles}" var="entry">
							<label class="radio inline">
								<input type="radio" name="${settingId}" id="${settingId}" value="${entry.key}"
									<c:if test='${settingValue eq  entry.key}'>checked="checked"</c:if>
									/>
								${entry.value}
							</label>							
							<c:set var="help">
								<setup:message key='${entry.key}.desc' messageSource='${provider.messageSource}'/>
							</c:set>
			
							<c:if test="${fn:length(help) > 0}">
								<button type="button" class=" help-popover help-icon" tabindex="-1"
										data-content="${fn:escapeXml(help)}"
										data-html="true">
									<i class="icon-question-sign"></i>
								</button>
							</c:if>
							<br/>
						</c:forEach>
						<script>
						$(function() {
							SolarNode.Settings.addRadio({
								key: '${settingId}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.MultiValueSettingSpecifier')}">
						<select name="${settingId}" id="${settingId}">
							<c:forEach items="${setting.valueTitles}" var="entry">
								<option value="${entry.key}"
										<c:if test='${settingValue eq  entry.key}'>selected="selected"</c:if>
								>
									${entry.value}
								</option>
							</c:forEach>
						</select>
						<script>
						$(function() {
							SolarNode.Settings.addSelect({
								key: '${settingId}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TextFieldSettingSpecifier')}">
						<input type="${setting.secureTextEntry == true ? 'password' : 'text' }" name="${settingId}" id="${settingId}" 
							class="span5" maxLength="255"
							<c:choose>
								<c:when test='${setting.secureTextEntry == true}'>
									placeholder="<fmt:message key='settings.secureTextEntry.placeholder'/>"
								</c:when>
								<c:otherwise>
									value="${settingValue}"
								</c:otherwise>
							</c:choose>
							/>
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
						<span class="title">${fn:escapeXml(settingValue)}</span>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.LocationLookupSettingSpecifier')}">
						<span id="${settingId}">
							<span class="setting-value">
								<c:if test="${not empty setting.sourceName}">
									<fmt:message key="lookup.selected.item">
										<fmt:param value="${setting.locationName}"/>
										<fmt:param value="${setting.sourceName}"/>
									</fmt:message>
								</c:if>
							</span>
							<button type="button" class="btn">
								<fmt:message key="settings.change"/>
							</button>
						</span>
						<script>
						$(function() {
							SolarNode.Settings.addLocationFinder({
								key: '${settingId}',
								locationType: '${setting.locationTypeKey}',
								sourceName: '${setup:js(setting.sourceName)}',
								locationName: '${setup:js(setting.locationName)}',
								valueLabel: '<fmt:message key="lookup.selected.item"/>',
								value: '${fn:escapeXml(settingValue)}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUID}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
				</c:choose>
				
				<c:set var="help">
					<setup:message key='${setting.key}.desc' messageSource='${provider.messageSource}' arguments='${setting.descriptionArguments}'/>
				</c:set>

				<c:if test="${fn:length(help) > 0}">
					<button type="button" class=" help-popover help-icon" tabindex="-1"
							data-content="${fn:escapeXml(help)}"
							data-html="true">
						<i class="icon-question-sign"></i>
					</button>
				</c:if>
				
				<span class="help-inline active-value clean"><span class="text-info">
					<c:choose>
						<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TextFieldSettingSpecifier') and setting.secureTextEntry == true}">
							<fmt:message key="settings.changed.value.label"/>
						</c:when>
						<c:otherwise>
							<fmt:message key="settings.current.value.label"/>:
							<code class="value">
								<c:choose>
									<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.ToggleSettingSpecifier')}">
								    	<c:choose>
								    		<c:when test="${settingValue eq  setting.trueValue}">
								    			<fmt:message key="settings.toggle.on"/>
								    		</c:when>
								    		<c:otherwise>
								    			<fmt:message key="settings.toggle.off"/>
								    		</c:otherwise>
								    	</c:choose>
									</c:when>
									<c:otherwise>
										${settingValue}
									</c:otherwise>
								</c:choose>
							</code>
						</c:otherwise>
					</c:choose>
				</span></span>
			</div>
		</div>
	</c:when>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.GroupSettingSpecifier') and not empty setting.key}">
		<div class="control-group grouped">
			<label class="control-label">
				<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}" text="${setting.key}"/>
			</label>
			<div class="controls">
				<c:if test="${setting.dynamic}">
					<div class="btn-group btn-group-sm" role="group">
						<button type="button" class="btn btn-small btn-default group-item-remove">
							<i class="icon-minus"></i>
						</button>
						<button type="button" class="btn btn-small btn-default group-item-add">
							<i class="icon-plus"></i>
						</button>

						<c:set var="help">
							<setup:message key='${setting.key}.desc' messageSource='${provider.messageSource}'/>
						</c:set>
						<c:if test="${fn:length(help) > 0}">
							<button type="button" class=" help-popover help-icon" tabindex="-1"
									data-content="${fn:escapeXml(help)}"
									data-html="true">
								<i class="icon-question-sign"></i>
							</button>
						</c:if>
					</div>
					<input type="hidden" name="${settingId}Count" id="${settingId}" value="${fn:length(setting.groupSettings)}" />
					<script>
					$(function() {
						SolarNode.Settings.addGroupedSetting({
							key: '${settingId}',
							provider: '${provider.settingUID}',
							setting: '${setup:js(setting.key)}Count',
							instance: '${instanceId}'
						});
					});
					</script>
				</c:if>
			</div>
		</div>
		<fieldset id="${settingId}g">
			<c:if test="${not empty setting.groupSettings}">
				<c:set var="origSetting" value="${setting}"/>
				<c:set var="origSettingId" value="${settingId}"/>
				<c:forEach items="${setting.groupSettings}" var="groupedSetting" varStatus="groupedSettingStatus">
					<c:set var="setting" value="${groupedSetting}" scope="request"/>
					<c:set var="settingId" value="${origSettingId}g${groupedSettingStatus.index}" scope="request"/>
					<c:set var="groupSettingId" value="${origSettingId}" scope="request"/>
					<c:set var="groupSetting" value="${origSetting}" scope="request"/>
					<c:set var="groupIndex" value="${groupedSettingStatus.count}" scope="request"/>
					<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
				</c:forEach>
				<c:remove var="groupSettingId" scope="request"/>
				<c:remove var="groupSetting" scope="request"/>
				<c:remove var="groupIndex" scope="request"/>
				<c:set var="setting" value="${origSetting}" scope="request"/>
				<c:set var="settingId" value="${origSettingId}" scope="request"/>
			</c:if>
		</fieldset>
	</c:when>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.GroupSettingSpecifier')}">
		<c:if test="${not empty setting.groupSettings}">
			<fieldset>
				<c:set var="origSetting" value="${setting}"/>
				<c:set var="origSettingId" value="${settingId}"/>
				<c:forEach items="${setting.groupSettings}" var="groupedSetting" varStatus="groupedSettingStatus">
					<c:set var="setting" value="${groupedSetting}" scope="request"/>
					<c:set var="settingId" value="${origSettingId}g${groupedSettingStatus.index}" scope="request"/>
					<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
				</c:forEach>
				<c:set var="setting" value="${origSetting}" scope="request"/>
				<c:set var="settingId" value="${origSettingId}" scope="request"/>
			</fieldset>
		</c:if>
	</c:when>
</c:choose>
