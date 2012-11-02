<section class="intro">
	<p>
		<fmt:message key="settings.intro"/>
	</p>
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
			<c:set var="provider" value="${provider}" scope="request"/>
			<fieldset>
				<legend><setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/></legend>
				<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
					<c:set var="settingId" value="s${providerStatus.index}i${settingStatus.index}" scope="request"/>
					<c:set var="setting" value="${setting}" scope="request"/>
					<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
				</c:forEach>
			</fieldset>
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
		SolarNode.Settings.reset();
	});
	</script>
</c:if>
