<c:if test="${fn:length(providers) > 0}">
	<section id="settings">
		<h2>
			<a id="settings-section" href="#settings-section"
				class="anchor" aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
			<fmt:message key="settings.providers.title"/>
		</h2>
		<p><fmt:message key="settings.providers.intro"/></p>

		<form action="<setup:url value='/a/settings/save'/>" method="post">
			<div class="form-actions d-grid my-5">
				<button type="button" class="btn btn-primary settings-save" id="submit" disabled><fmt:message key='settings.save'/></button>
			</div>

			<c:forEach items="${providers}" var="provider" varStatus="providerStatus">
				<!--  ${provider.settingUid} -->
				<c:set var="provider" value="${provider}" scope="request"/>
				<fieldset>
					<legend>
						<a id="${provider.settingUid}"
							class="anchor"
							href="#${provider.settingUid}"
							aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
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
					<c:if test="${not empty settingResources[provider.settingUid]}">
					<div class="row mb-3">
						<label class="col-sm-4 col-md-3 col-form-label" for="settings-resource-ident-${providerStatus.index}">
							<fmt:message key="settings.io.exportResource.label"/>
						</label>
						<div class="col-sm-7 col-md-8">
							<div class="input-group">
								<select class="form-select settings-resource-ident" id="settings-resource-ident-${providerStatus.index}">
									<c:forEach items="${settingResources[provider.settingUid]}" var="resource">
										<option data-handler="${resource.handlerKey}" data-key="${resource.key}">${resource.name}</option>
									</c:forEach>
								</select>
	  							<button type="button" class="btn btn-primary settings-resource-export"
	  									data-action="<setup:url value='/a/settings/exportResources'/>"
										data-target="#settings-resource-ident-${providerStatus.index}"
	  								><fmt:message key="settings.io.export.button"/></button>
  							</div>
						</div>
					</div>
					</c:if>
				</fieldset>
			</c:forEach>
			<sec:csrfInput/>
		</form>
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
		});
		</script>
	</section>
</c:if>
<jsp:include page="note-popover.jsp"/>