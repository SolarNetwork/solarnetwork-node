<div class="intro">
	<fmt:message key="settings.intro"/>
</div>

<c:if test="${fn:length(factories) > 0}">
	<table class="factories">
		<tbody>
			<c:forEach items="${factories}" var="factory" varStatus="factoryStatus">
				<!--  ${factory.factoryUID} -->
				<tr>
					<td>
						<setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/>
					</td>
					<td>
						<form action="<c:url value='/settings/manage.do'/>" method="get">
							<input type="hidden" name="uid" value="${factory.factoryUID}"/>
							<button type="submit"><fmt:message key="settings.factory.manage.label"/></button>
						</form>
					</td>
				</tr>
			</c:forEach>
		</tbody>
	</table>
</c:if>

<c:if test="${fn:length(providers) > 0}">
	<form id="settings-form" action="<c:url value='/settings/save.do'/>" method="post">
	<table class="settings">
		<c:forEach items="${providers}" var="provider" varStatus="providerStatus">
			<thead>
				<!--  ${provider.settingUID} -->
				<tr>
					<td colspan="2">
						<setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/>
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
											<div id="s${providerStatus.index}i${settingStatus.index}"
												 class="setting slider"></div>
											<script>
											$(function() {
												SolarNode.Settings.addSlider({
													key: 's${providerStatus.index}i${settingStatus.index}',
													min: '${setting.minimumValue}',
													max: '${setting.maximumValue}',
													step: '${setting.step}',
													value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>',
													provider: '${provider.settingUID}',
													setting: '${setting.key}'
												});
											});
											</script>
										</c:when>
										<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.ToggleSettingSpecifier')}">
											<div id="s${providerStatus.index}i${settingStatus.index}" class="setting toggle">
												<input type="radio" name="s${providerStatus.index}i${settingStatus.index}" id="s${providerStatus.index}i${settingStatus.index}t" value="${setting.trueValue}" />
													<label for="s${providerStatus.index}i${settingStatus.index}t"><fmt:message key="settings.toggle.on"/></label>
												<input type="radio" name="s${providerStatus.index}i${settingStatus.index}" id="s${providerStatus.index}i${settingStatus.index}f" value="${setting.falseValue}" />
													<label for="s${providerStatus.index}i${settingStatus.index}f"><fmt:message key="settings.toggle.off"/></label>
											</div>
											<script>
											$(function() {
												SolarNode.Settings.addToggle({
													provider: '${provider.settingUID}',
													setting: '${setting.key}',
													key: 's${providerStatus.index}i${settingStatus.index}',
													on: '${setting.trueValue}',
													off: '${setting.falseValue}',
													value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>'
												});
											});
											</script>
										</c:when>
										<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TextFieldSettingSpecifier')}">
											<input type="text" name="s${providerStatus.index}i${settingStatus.index}" id="s${providerStatus.index}i${settingStatus.index}" 
												value="<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>" />
											<script>
											$(function() {
												SolarNode.Settings.addTextField({
													provider: '${provider.settingUID}',
													setting: '${setting.key}',
													key: 's${providerStatus.index}i${settingStatus.index}'
												});
											});
											</script>
										</c:when>
										<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TitleSettingSpecifier')}">
											<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
										</c:when>
									</c:choose>
								</td>
								<td class="value" id="s${providerStatus.index}i${settingStatus.index}v">
									<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
								</td>
								<td class="description">
									<div class="description-dialog" id="d${providerStatus.index}i${settingStatus.index}">
										<setup:message key="${setting.key}.desc" messageSource="${provider.messageSource}"/>
									</div>
								</td>
							</tr>
							<script>
							$(function() {
								SolarNode.Settings.addInfoDialog({
									key: 'd${providerStatus.index}i${settingStatus.index}',
									title: '<setup:message key="${setting.key}.key" messageSource="${provider.messageSource}"/>'
								});
							});
							</script>
						</c:when>
					</c:choose>
				</c:forEach>
			</tbody>
		</c:forEach>
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
		SolarNode.Settings.reset();
	});
	</script>
</c:if>
