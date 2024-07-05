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
	<setup:settingValue service='${settingsService}' provider='${provider}' setting='${setting}'
		escapeXml="${setup:instanceOf(setting, 'net.solarnetwork.settings.MarkupSetting')
			? !setting.markup : true}"/>
</c:set>
<c:choose>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.SetupResourceSettingSpecifier')}">
		<setup:resources role="USER" type="text/html" inline="true"
			provider="${setting.setupResourceProvider}"
			properties="${setting.setupResourceProperties}"
			wrapperElement="div"
			wrapperClass="form-group setup-resource-container"
			id="cg-${settingId}"
			data-provider-id="${provider.settingUid}"
			data-setting-id="${settingId}"
			data-instance-id="${instanceId}"
			data-group-index="${groupIndex}"
			/>
	</c:when>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.KeyedSettingSpecifier')}">
		<div class="row mb-3" id="cg-${settingId}">
			<label class="col-sm-4 col-md-3 col-form-label" for="${settingId}">
				<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}" text="${setting.key}" index="${groupIndex}"/>
			</label>
			<div class="col-sm-7 col-md-8">
				<c:choose>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.SliderSettingSpecifier')}">
						<div id="${settingId}" class="setting slider mt-2"></div>
						<script>
						$(function() {
							SolarNode.Settings.addSlider({
								key: '${settingId}',
								min: '${setting.minimumValue}',
								max: '${setting.maximumValue}',
								step: '${setting.step}',
								value: '${fn:escapeXml(settingValue)}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUid}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.ToggleSettingSpecifier')}">
					    <button type="button" class="toggle col-sm-3 btn ${settingValue eq  setting.trueValue ? 'btn-success active' : 'btn-light'}"
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
								provider: '${provider.settingUid}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.RadioGroupSettingSpecifier')}">
						<c:forEach items="${setting.valueTitles}" var="entry">
							<div class="d-flex justify-content-start align-items-center mt-1">
								<div class="form-check">
									<input class="form-check-input" type="radio" name="${settingId}" id="${settingId}" value="${entry.key}"
										<c:if test='${settingValue eq  entry.key}'>checked="checked"</c:if>
										>
									<label class="form-check-label" for="${settingId}">${entry.value}</label>
								</div>

								<c:set var="help">
									<setup:message key='${entry.key}.desc' messageSource='${provider.messageSource}'/>
								</c:set>
								<c:if test="${fn:length(help) > 0}">
									<button type="button" class="help-popover help-icon ms-2" title="<fmt:message key='help.label'/>" tabindex="-1"
											data-bs-content="${fn:escapeXml(help)}"
											data-bs-html="true">
										<i class="bi bi-question-circle" aria-hidden="true"></i>
									</button>
								</c:if>
							</div>
						</c:forEach>
						<script>
						$(function() {
							SolarNode.Settings.addRadio({
								key: '${settingId}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUid}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.MultiValueSettingSpecifier')}">
						<select class="form-select" name="${settingId}" id="${settingId}">
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
								provider: '${provider.settingUid}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.TextAreaSettingSpecifier')}">
						<div class="input-group">
							<textarea name="${settingId}" id="${settingId}" class="form-control">${settingValue}</textarea>
							<button type="button" class="btn btn-outline-secondary copy" tabindex="-1" title="<fmt:message key='copy.label'/>"><i class="bi bi-clipboard2"></i></button>
						</div>
						<c:choose>
							<c:when test="${setting.direct}">
								<script>
								$(function() {
									SolarNode.Settings.addTextField({
										key: '${settingId}',
										xint: '${setting["transient"]}',
										provider: '${provider.settingUid}',
										setting: '${setup:js(setting.key)}',
										instance: '${instanceId}'
									});
								});
								</script>
							</c:when>
							<c:otherwise>
								<button type="button" class="btn btn-primary mt-2 setting-resource-upload"
									data-action="<setup:url value='/a/settings/importResource'/>"
									data-key="${settingId}"
									data-xint="${setting['transient']}"
									data-provider="${provider.settingUid}"
									data-setting="${setup:js(setting.key)}"
									data-instance="${instanceId}"
									>
									<fmt:message key="settings.resource.upload.action"/>
								</button>
							</c:otherwise>
						</c:choose>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.TextFieldSettingSpecifier')}">
						<div class="row">
							<c:if test="${setup:js(setting.key) == 'schedule'}">
								<div class="col-sm-3">
									<select class="form-select">
										<option value="cron"><fmt:message key='settings.schedulePeriod.cron.label'/></option>
										<option value="ms"><fmt:message key='settings.schedulePeriod.milliseconds.label'/></option>
										<option value="s"><fmt:message key='settings.schedulePeriod.seconds.label'/></option>
										<option value="m"><fmt:message key='settings.schedulePeriod.minutes.label'/></option>
										<option value="h"><fmt:message key='settings.schedulePeriod.hours.label'/></option>
									</select>
								</div>
							</c:if>
							<div class="col">
								<div class="input-group">
									<input type="${setting.secureTextEntry == true ? 'password' : 'text' }" name="${settingId}" id="${settingId}"
										class="form-control" maxLength="4096"
										<c:choose>
											<c:when test='${setting.secureTextEntry == true}'>
												placeholder="<fmt:message key='settings.secureTextEntry.placeholder'/>"
											</c:when>
											<c:otherwise>
												value="${settingValue}"
											</c:otherwise>
										</c:choose>
										/>
									<c:if test="${not empty setting.relatedServiceFilter}">
									  <button class="btn btn-outline-secondary dropdown-toggle" type="button" 
									  		data-bs-toggle="dropdown" aria-expanded="false"><fmt:message key='settings.relatedServiceFilter.choose.label'/></button>
										<ul class="dropdown-menu dropdown-menu-end setting-related-service" data-setting-related-service-filter="${setting.relatedServiceFilter}">
											<!-- Dynamically populated -->
										</ul>
									</c:if>
									<c:if test="${setting.secureTextEntry != true}">
										<button type="button" class="btn btn-outline-secondary copy" tabindex="-1" title="<fmt:message key='copy.label'/>"><i class="bi bi-clipboard2"></i></button>
									</c:if>
								</div>
							</div>
						</div>
						<script>
						$(function() {
						<c:choose>
							<c:when test="${setup:js(setting.key) == 'schedule'}">
							SolarNode.Settings.addScheduleField({
							</c:when>
							<c:otherwise>
							SolarNode.Settings.addTextField({
							</c:otherwise>
						</c:choose>
								key: '${settingId}',
								xint: '${setting["transient"]}',
								provider: '${provider.settingUid}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.TitleSettingSpecifier')}">
						<div style="margin-top: calc(0.375rem + 1px);">${settingValue}</div>
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
							<button type="button" class="btn btn-primary">
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
								provider: '${provider.settingUid}',
								setting: '${setup:js(setting.key)}',
								instance: '${instanceId}'
							});
						});
						</script>
					</c:when>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.FileSettingSpecifier')}">
						<c:set var="acceptFileTypes">
							<c:forEach items="${setting.acceptableFileTypeSpecifiers}" var="fileType" varStatus="fileTypeStatus">
								<c:if test="${!fileTypeStatus.first}">,</c:if><c:out value="${fileType}" />
							</c:forEach>
						</c:set>
						<div class="input-group">
							<input type="file" name="${settingId}" id="${settingId}" class="form-control"
								<c:if test="${fn:length(acceptFileTypes) gt 0}">
									accept="${acceptFileTypes}"
								</c:if>
								<c:if test="${setting.multiple}">
									multiple="multiple"
								</c:if>
								/>
							<button type="button" class="btn btn-primary setting-resource-upload"
								data-action="<setup:url value='/a/settings/importResource'/>"
								data-key="${settingId}"
								data-xint="${setting['transient']}"
								data-provider="${provider.settingUid}"
								data-setting="${setup:js(setting.key)}"
								data-instance="${instanceId}"
								data-multiple="${!!setting.multiple}"
								><fmt:message key="settings.resource.upload.action"/></button>
						</div>
					</c:when>
				</c:choose>


				<span class="help-inline active-value clean"><span class="text-info">
					<c:choose>
						<c:when test="${(setup:instanceOf(setting, 'net.solarnetwork.settings.TextFieldSettingSpecifier') and setting.secureTextEntry == true)
									|| setup:instanceOf(setting, 'net.solarnetwork.settings.TextAreaSettingSpecifier')
									|| setup:instanceOf(setting, 'net.solarnetwork.node.settings.FileSettingSpecifier')}">
							<fmt:message key="settings.changed.value.label"/>
						</c:when>
						<c:otherwise>
							<fmt:message key="settings.current.value.label"/>:
							<code class="value">
								<c:choose>
									<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.ToggleSettingSpecifier')}">
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
			<div class="col-sm-1 mt-1">
				<c:set var="help">
					<setup:message key='${setting.key}.desc' messageSource='${provider.messageSource}' arguments='${setting.descriptionArguments}'/>
				</c:set>

				<c:if test="${fn:length(help) gt 0}">
					<button type="button" class="help-popover help-icon" title="<fmt:message key='help.label'/>" tabindex="-1"
							data-bs-content="${fn:escapeXml(help)}"
							data-bs-html="true">
						<i class="bi bi-question-circle" aria-hidden="true"></i>
					</button>
				</c:if>
			</div>
		</div>
	</c:when>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.GroupSettingSpecifier') and not empty setting.key}">
		<div class="row grouped mb-3">
			<label class="col-sm-4 col-md-3 col-form-label">
				<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}" text="${setting.key}"/>
			</label>
			<div class="col-sm-7 col-md-8">
				<c:if test="${setting.dynamic}">
					<div class="btn-group" role="group">
						<button type="button" class="btn btn-primary group-item-remove">
							<i class="bi bi-dash-lg"></i>
						</button>
						<button type="button" class="btn btn-primary group-item-add">
							<i class="bi bi-plus-lg"></i>
						</button>
					</div>
					<input type="hidden" name="${settingId}Count" id="${settingId}" value="${fn:length(setting.groupSettings)}" />
					<script>
					$(function() {
						SolarNode.Settings.addGroupedSetting({
							key: '${settingId}',
							provider: '${provider.settingUid}',
							setting: '${setup:js(setting.key)}Count',
							instance: '${instanceId}',
							indexed: '${setup:js(setting.key)}'
						});
					});
					</script>
				</c:if>
			</div>
			<div class="col-sm-1 mt-1">
				<c:set var="help">
					<setup:message key='${setting.key}.desc' messageSource='${provider.messageSource}'/>
				</c:set>
				<c:if test="${fn:length(help) > 0}">
					<button type="button" class=" help-popover help-icon" title="<fmt:message key='help.label'/>" tabindex="-1"
							data-bs-content="${fn:escapeXml(help)}"
							data-bs-html="true">
						<i class="bi bi-question-circle" aria-hidden="true"></i>
					</button>
				</c:if>
			</div>
		</div>
		<c:if test="${not empty setting.groupSettings}">
			<fieldset id="${settingId}g" class="pt-3 grouped">
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
			</fieldset>
		</c:if>
	</c:when>
	<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.settings.GroupSettingSpecifier')}">
		<c:if test="${not empty setting.groupSettings}">
			<fieldset class="group">
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
