<section class="intro">
	<fmt:message key="settings.intro"/>
</section>

<c:if test="${fn:length(factories) > 0}">
	<section id="factories">
		<h2><fmt:message key="settings.factories.title"/></h2>
		<p><fmt:message key="settings.factories.intro"/></p>	
		<table class="table">
			<tbody>
			<c:forEach items="${factories}" var="factory" varStatus="factoryStatus">
				<!--  ${factory.factoryUID} -->
				<tr>
					<td><strong><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></strong></td>
					<td>
						<a class="btn" href="<c:url value='/settings/manage.do?uid=${factory.factoryUID}'/>">
							<i class="icon-edit icon-large"></i> 
							<fmt:message key="settings.factory.manage.label"/>
						</a>
					</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>

<c:if test="${fn:length(providers) > 0}">
	<section id="settings">
		<h2><fmt:message key="settings.providers.title"/></h2>
		<p><fmt:message key="settings.providers.intro"/></p>	

		<form class="form-horizontal" action="<c:url value='/settings/save.do'/>" method="post">
		<c:forEach items="${providers}" var="provider" varStatus="providerStatus">
			<!--  ${provider.settingUID} -->
			<fieldset>
				<legend><setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/></legend>
				<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
					<c:set var="settingId" value="s${providerStatus.index}i${settingStatus.index}"/>
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
													provider: '${provider.settingUID}',
													setting: '${setting.key}'
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
													provider: '${provider.settingUID}',
													setting: '${setting.key}',
													key: '${settingId}',
													on: '${setting.trueValue}',
													off: '${setting.falseValue}',
													value: '<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>'
												});
											});
											</script>
										</c:when>
										<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TextFieldSettingSpecifier')}">
											<input type="text" name="${settingId}" id="${settingId}" 
												value="<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>" />
											<script>
											$(function() {
												SolarNode.Settings.addTextField({
													provider: '${provider.settingUID}',
													setting: '${setting.key}',
													key: '${settingId}'
												});
											});
											</script>
										</c:when>
										<c:when test="${setup:instanceOf(setting, 'net.solarnetwork.node.settings.TitleSettingSpecifier')}">
											<setup:settingValue service="${settingsService}" provider="${provider}" setting="${setting}"/>
										</c:when>
									</c:choose>
									
									<button type="button" class=" help-popover help-icon" tabindex="-1"
											data-content="<setup:message key='${setting.key}.desc' messageSource='${provider.messageSource}'/>"
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
				</c:forEach>
			</fieldset>
		</c:forEach>
			<div class="actions">
				<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
			</div>
		</form>
	</section>
<%--
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
--%>
	<script>
	$(function() {
		$('#submit').click(function() {
			SolarNode.Settings.saveUpdates($(this.form).attr('action'), {
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
