<p class="lead"><fmt:message key="new-node.intro"/></p>

<c:set var="myNodesURL" value='${networkLinks["solaruser"]}/u/sec/my-nodes'/>
<p><fmt:message key="node.setup.code.intro">
	<fmt:param value="${myNodesURL}"/>
</fmt:message></p>

<p><fmt:message key="node.setup.restore.option">
	<fmt:param><setup:url value="/associate/restore"/></fmt:param>
</fmt:message></p>

<setup:url value="/associate/preview" var="action"/>
<form:form action="${action}" method="post" cssClass="form-horizontal">
	<form:errors cssClass="alert alert-danger" element="div" htmlEscape="false"/>
	<fieldset>
		<c:set var="err"><form:errors path="verificationCode" cssClass="help-inline" element="span"/></c:set>
		<div class="form-group<c:if test='${not empty err}'> error</c:if>">
			<label class="control-label" for="${settingId}">
				<fmt:message key="node.setup.code.verificationCode"/>
			</label>
			<div class="controls">
				<fmt:message key='node.setup.code.verificationCode.placeholder' var="placeholder"/>
				<form:textarea path="verificationCode" placeholder="${placeholder}" rows="10" cssClass="col-md-9" required="required"/>
				<c:out value="${err}" escapeXml="false"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<button type="submit" class="btn btn-primary"><fmt:message key='node.setup.code.verify'/></button>
	</div>
	<sec:csrfInput/>
</form:form>

<c:if test="${fn:length(providers) > 0}">
	<section id="settings">
		<h2>
			<a id="settings-section" href="#settings-section"
				class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
			<fmt:message key="node.setup.settings.providers.title"/>
		</h2>
		<p><fmt:message key="node.setup.settings.providers.intro"/></p>	

		<form class="form-horizontal" action="<setup:url value='/associate/configure'/>" method="post">
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
			<div class="form-actions">
				<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
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
