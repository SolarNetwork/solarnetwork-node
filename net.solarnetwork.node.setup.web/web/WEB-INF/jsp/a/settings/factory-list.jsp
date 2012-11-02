<section class="intro">
	<h2>
		<fmt:message key="settings.factory.title">
			<fmt:param value="${factory.displayName}"/>
		</fmt:message>
	</h2>
	<p>
		<fmt:message key="settings.factory.intro">
			<fmt:param value="${factory.displayName}"/>
		</fmt:message>
	</p>
	<p>
		<a href="<c:url value='/settings.do'/>" class="btn">
			<i class="icon-arrow-left"></i>
			<fmt:message key="back.label"/>
		</a>
		<button type="button" class="btn btn-primary" id="add">
			<i class="icon-plus icon-white"></i>
			<fmt:message key='settings.factory.add'>
				<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
			</fmt:message>
		</button>
	</p>
</section>

<section id="settings">
	<form class="form-horizontal" action="<c:url value='/settings/save.do'/>" method="post">
		<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
			<c:set var="instance" value="${instance}" scope="request"/>
			<c:forEach items="${instance.value}" var="provider" varStatus="providerStatus">
				<c:set var="provider" value="${provider}" scope="request"/>
				<c:set var="instanceId" value="${provider.factoryInstanceUID}" scope="request"/>
				<!--  ${provider.settingUID} -->
		
					<fieldset>
						<legend>
							<setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/>
							${instance.key}
						</legend>
						
						<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
							<c:set var="setting" value="${setting}" scope="request"/>
							<c:set var="settingId" value="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" scope="request"/>
							<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
						</c:forEach>
						<div class="control-group">
							<div class="controls">
								<button type="button" class="btn btn-danger" id="del${instance.key}">
									<fmt:message key='settings.factory.delete'>
										<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
										<fmt:param value="${instance.key}"/>
									</fmt:message>
								</button>
								<script>
								$('#del${instance.key}').click(function() {
									SolarNode.Settings.deleteFactoryConfiguration({
										button: this,
										url: '<c:url value="/settings/manage/delete.do"/>',
										factoryUID: '${factory.factoryUID}',
										instanceUID: '${instance.key}'
									});
								});
								</script>
							</div>
						</div>
					</fieldset>
			</c:forEach>
		</c:forEach>
		<div class="form-actions">
			<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
		</div>
	</form>
</section>
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
	$('#add').click(function() {
		SolarNode.Settings.addFactoryConfiguration({
			button: this,
			url: '<c:url value="/settings/manage/add.do"/>',
			factoryUID: '${factory.factoryUID}'
		});
	});
	SolarNode.Settings.reset();
});
</script>
<div id="alert-delete" class="alert alert-danger alert-block hidden">
	<button type="button" class="close" data-dismiss="alert">×</button>
	<h4><fmt:message key="settings.factory.delete.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.delete.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="delete.label"/>
	</button>
</div>

<%--
<form action="<c:url value='/settings/save.do'/>" method="post">
<table class="settings">
<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
	<c:set var="instance" value="${instance}" scope="request"/>
	<c:forEach items="${instance.value}" var="provider" varStatus="providerStatus">
		<c:set var="provider" value="${provider}" scope="request"/>
		<c:set var="instanceId" value="${provider.factoryInstanceUID}" scope="request"/>
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
				<c:set var="setting" value="${setting}" scope="request"/>
				<c:set var="settingId" value="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" scope="request"/>
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
</form>
<div class="alert" id="alert-delete">
	<fmt:message key="settings.factory.delete.alert.msg"/>
</div>
--%>
