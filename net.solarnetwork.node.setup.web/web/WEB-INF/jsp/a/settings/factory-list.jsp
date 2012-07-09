<div class="intro">
	<form method="get" action="<c:url value='/settings.do'/>">
		<table>
			<tbody>
				<tr>
					<td>Managing settings for ${factory.displayName}.</td>
					<td><button type="submit"><fmt:message key="back.label"/></button></td>
				</tr>
			</tbody>
		</table>
	</form>
</div>

<form id="settings-form" action="<c:url value='/settings/save.do'/>" method="post">
<table class="settings">
<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
	<c:forEach items="${instance.value}" var="provider" varStatus="providerStatus">
		<thead>
			<!--  ${provider.settingUID} -->
			<tr>
				<td colspan="2">
					<setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/>
					${instance.key}
				</td>
				<td>
					<fmt:message key="settings.current.value.label"/>
				</td>
				<td class="description">
					<fmt:message key="settings.description.label"/>
				</td>
			</tr>
		</thead>
		<tbody>
			<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
				<c:choose>
					<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.KeyedSettingSpecifier')}">
						<tr>
							<td class="label"><setup:message key="${setting.key}.key" messageSource="${provider.messageSource}" text="${setting.key}"/></td>
							<td class="setting">
								<c:choose>
									<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.SliderSettingSpecifier')}">
										<div id="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}"
											 class="setting slider"></div>
										<script>
										$(function() {
											SolarNode.Settings.addSlider({
												key: 'm${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}',
												min: '${setting.minimumValue}',
												max: '${setting.maximumValue}',
												step: '${setting.step}',
												value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>',
												xint: '${setting["transient"]}',
												provider: '${provider.settingUID}',
												setting: '${setup:js(setting.key)}',
												instance: '${provider.factoryInstanceUID}'
											});
										});
										</script>
									</c:when>
									<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.ToggleSettingSpecifier')}">
										<div id="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" class="setting toggle">
											<input type="radio" name="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" id="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}t" value="${setting.trueValue}" />
												<label for="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}t"><fmt:message key="settings.toggle.on"/></label>
											<input type="radio" name="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" id="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}f" value="${setting.falseValue}" />
												<label for="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}f"><fmt:message key="settings.toggle.off"/></label>
										</div>
										<script>
										$(function() {
											SolarNode.Settings.addToggle({
												provider: '${provider.settingUID}',
												setting: '${setup:js(setting.key)}',
												instance: '${provider.factoryInstanceUID}',
												key: 'm${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}',
												on: '${setting.trueValue}',
												off: '${setting.falseValue}',
												value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>',
												xint: '${setting["transient"]}'
											});
										});
										</script>
									</c:when>
									<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TextFieldSettingSpecifier')}">
										<input type="text" name="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" 
											id="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" 
											value="<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>" />
										<script>
										$(function() {
											SolarNode.Settings.addTextField({
												provider: '${provider.settingUID}',
												setting: '${setup:js(setting.key)}',
												instance: '${provider.factoryInstanceUID}',
												key: 'm${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}',
												xint: '${setting["transient"]}'
											});
										});
										</script>
									</c:when>
									<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TitleSettingSpecifier')}">
										<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
									</c:when>
									<c:otherwise>
									TODO: ${setting['class']}
									</c:otherwise>
								</c:choose>
							</td>
							<td class="value" id="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}v">
								<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
							</td>
							<td class="description">
								<div class="description-dialog" id="m${instanceStatus.index}d${providerStatus.index}i${settingStatus.index}">
									<setup:message key="${setting.key}.desc" messageSource="${provider.messageSource}"/>
								</div>
							</td>
						</tr>
						<script>
						$(function() {
							SolarNode.Settings.addInfoDialog({
								key: 'm${instanceStatus.index}d${providerStatus.index}i${settingStatus.index}',
								title: '<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}"/>'
							});
						});
						</script>
					</c:when>
				</c:choose>
			</c:forEach>
			<tr>
				<td colspan="3">
					<button type="button" id="del${instance.key}" class="delete">
						<fmt:message key='settings.factory.delete'>
							<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
							<fmt:param value="${instance.key}"/>
						</fmt:message>
					</button>
					<script>
					$('#del${instance.key}').click(function() {
						SolarNode.Settings.deleteFactoryConfiguration({
							url: '<c:url value="/settings/manage/delete.do"/>',
							factoryUID: '${factory.factoryUID}',
							instanceUID: '${instance.key}',
							'title': '<fmt:message key="settings.factory.delete.alert.title"/>',
							'delete.label': '<fmt:message key="delete.label"/>',
							'cancel.label': '<fmt:message key="cancel.label"/>'
						});
					});
					</script>
				</td>
			</tr>
		</tbody>
	</c:forEach>
</c:forEach>
	<tbody>
		<tr>
			<td colspan="3">
				<button type="button" id="add">
					<fmt:message key='settings.factory.add'>
						<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
					</fmt:message>
				</button>
			</td>
		</tr>
	</tbody>
</table>
<div class="actions">
	<button type="button" id="submit"><fmt:message key='settings.save'/></button>
</div>
</form>
<script>
$(function() {
	$('#submit').click(function() {
		SolarNode.Settings.saveUpdates($('#settings-form').attr('action'), {
			success: '<fmt:message key="settings.save.success.msg"/>',
			error: '<fmt:message key="settings.save.error.msg"/>',
			title: '<fmt:message key="settings.save.result.title"/>',
			button: '<fmt:message key="ok.label"/>'
		});
	});
	$('#add').click(function() {
		SolarNode.Settings.addFactoryConfiguration({
			url: '<c:url value="/settings/manage/add.do"/>',
			factoryUID: '${factory.factoryUID}'
		});
	});
	SolarNode.Settings.reset();
});
</script>

<div class="alert" id="alert-delete">
	<fmt:message key="settings.factory.delete.alert.msg"/>
</div>
