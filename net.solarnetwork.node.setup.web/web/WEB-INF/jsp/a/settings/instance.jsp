<%--
	Inputs:
	
	factory 			- the provider factory
	instance 			- Entry< instanceId, FactorySettingSpecifierProvider> ???
	instanceId			- instance ID
	instanceStatus		- instance iterator status (e.g. instanceStatus.index)
	provider 			- instance.value (FactorySettingSpecifierProvider)
	settingsService		- the settings service
 --%>
<c:catch var="providerException">
	<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
		<c:set var="setting" value="${setting}" scope="request"/>
		<c:set var="settingId" value="m${instanceStatus.index}s0i${settingStatus.index}" scope="request"/>
		<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
	</c:forEach>
</c:catch>
<c:if test="${not empty providerException}">
	<div class="alert alert-warning">
		<fmt:message key="settings.error.provider.exception">
			<fmt:param value="${providerException.cause.message}"/>
		</fmt:message>
	</div>
</c:if>