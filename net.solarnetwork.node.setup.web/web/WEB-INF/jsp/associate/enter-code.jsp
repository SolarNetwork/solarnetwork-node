<p class="lead"><fmt:message key="new-node.intro"/></p>

<section>
	<c:set var="myNodesURL" value='${networkLinks["solaruser"]}/u/sec/my-nodes'/>
	<p><fmt:message key="node.setup.code.intro">
		<fmt:param value="${myNodesURL}"/>
	</fmt:message></p>
	
	<p><fmt:message key="node.setup.restore.option">
		<fmt:param><setup:url value="/associate/restore"/></fmt:param>
	</fmt:message></p>
	
	<setup:url value="/associate/preview" var="action"/>
	<form:form action="${action}" method="post">
		<form:errors cssClass="alert alert-danger" element="div" htmlEscape="false"/>
		<c:set var="err"><form:errors path="verificationCode" cssClass="form-text text-danger" element="div"/></c:set>
		<fieldset class="row g-3<c:if test='${not empty err}'> error</c:if>">
			<fmt:message key='node.setup.code.verificationCode.placeholder' var="placeholder"/>
			<form:textarea path="verificationCode" placeholder="${placeholder}" cssClass="form-control font-monospace"
				cssStyle="min-height: 10rem;" required="required" id="invitation-code"/>
			<c:out value="${err}" escapeXml="false"/>
			<button type="submit" class="btn btn-primary"><fmt:message key='node.setup.code.verify'/></button>
		</fieldset>
		<sec:csrfInput/>
	</form:form>
</section>

<c:if test="${fn:length(providers) > 0}">
	<section id="settings" style="margin-top: 4rem;">
		<h2>
			<a id="settings-section" href="#settings-section"
				class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
			<fmt:message key="node.setup.settings.providers.title"/>
		</h2>
		<p><fmt:message key="node.setup.settings.providers.intro"/></p>	

		<form action="<setup:url value='/associate/configure'/>" method="post">
			<c:forEach items="${providers}" var="provider" varStatus="providerStatus">
				<!--  ${provider.settingUid} -->
				<c:set var="provider" value="${provider}" scope="request"/>
				<fieldset>
					<legend>
						<a id="${provider.settingUid}" 
							class="anchor" 
							href="#${provider.settingUid}"
							aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>
						<setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/>
					</legend>
					<c:set var="providerDescription">
						<setup:message key="desc" messageSource="${provider.messageSource}" text=""/>
					</c:set>
					<c:if test="${fn:length(providerDescription) > 0}">
						<p>${providerDescription}</p>
					</c:if>
					<c:catch var="providerException">
						<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
							<c:set var="settingId" value="s${providerStatus.index}i${settingStatus.index}" scope="request"/>
							<c:set var="setting" value="${setting}" scope="request"/>
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
				</fieldset>
			</c:forEach>
			<div class="row my-3">
				<div class="col-sm-9 offset-sm-3">
					<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
				</div>
			</div>
			<sec:csrfInput/>
		</form>
	</section>
	<script>
	$(function() {
		$('#submit').on('click', function() {
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
