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
