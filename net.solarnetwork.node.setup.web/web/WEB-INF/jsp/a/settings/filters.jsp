<section class="intro">
	<p>
		<fmt:message key="filters.intro"/>
	</p>
</section>

<c:if test="${fn:length(globalFactories) > 0 or fn:length(providers) > 0}">
	<section id="global mb-3">
		<h2>
			<a id="global-section" href="#global-section"
				class="anchor" aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>			
			<fmt:message key="filters.global.title"/>
		</h2>
		<p><fmt:message key="filters.global.intro"/></p>

		<c:if test="${fn:length(globalFactories) > 0}">
			<div class="container">
				<div class="row setting-components">
					<div class="col">
						<c:forEach items="${globalFactories}" var="factory" varStatus="factoryStatus">
							<!--  ${factory.factoryUid} -->
							<div class="row my-3 justify-content-between align-items-center">
								<div class="col">
									<div class="row">
										<div class="col-2 col-sm-1">
											<span class="badge rounded-pill text-bg-primary${fn:length(factory.settingSpecifierProviderInstanceIds) lt 1 ? ' invisible' : ''}"
												title="<fmt:message key='settings.factories.instanceCount.caption'/>">${fn:length(factory.settingSpecifierProviderInstanceIds)}</span>
										</div>
										<div class="col"><strong><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></strong></div>
									</div>
								</div>
								<div class="col-auto">
									<a class="btn btn-light" href="<setup:url value='/a/settings/filters/manage?uid=${factory.factoryUid}'/>">
										<i class="bi bi-pencil-square"></i> 
										<fmt:message key="settings.factory.manage.label"/>
									</a>
								</div>
							</div>
						</c:forEach>
					</div>
				</div>
			</div>
		</c:if>
		
		<c:if test="${fn:length(providers) > 0}">
			<form action="<setup:url value='/a/settings/save'/>" class="mt-5" method="post">
				<div class="form-actions d-grid my-5">
					<button type="button" class="btn btn-primary settings-save" id="submit" disabled><fmt:message key='settings.save'/></button>
				</div>
				<c:forEach items="${providers}" var="provider" varStatus="providerStatus">
					<!--  ${provider.settingUid} -->
					<c:set var="provider" value="${provider}" scope="request"/>
					<fieldset class="mt-5">
						<h2>
							<a id="${provider.settingUid}" 
								class="anchor" 
								href="#${provider.settingUid}"
								aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
							<setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/>
						</h2>
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
				SolarNode.Settings.reset();
			});
			</script>			
		</c:if>
	</section>
</c:if>

<c:if test="${fn:length(userFactories) > 0}">
	<section id="user" class="mb-5">
		<h2>
			<a id="user-section" href="#user-section"
				class="anchor" aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>			
			<fmt:message key="filters.user.title"/>
		</h2>
		<p><fmt:message key="filters.user.intro"/></p>
		<div class="container">
			<div class="row setting-components">
				<div class="col">
					<c:forEach items="${userFactories}" var="factory" varStatus="factoryStatus">
						<!--  ${factory.factoryUid} -->
						<div class="row my-3 justify-content-between align-items-center">
							<div class="col">
								<div class="row">
									<div class="col-2 col-sm-1">
										<span class="badge rounded-pill text-bg-primary${fn:length(factory.settingSpecifierProviderInstanceIds) lt 1 ? ' invisible' : ''}"
											title="<fmt:message key='settings.factories.instanceCount.caption'/>">${fn:length(factory.settingSpecifierProviderInstanceIds)}</span>
									</div>
									<div class="col"><strong><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></strong></div>
								</div>
							</div>
							<div class="col-auto">
								<a class="btn btn-light" href="<setup:url value='/a/settings/filters/manage?uid=${factory.factoryUid}'/>">
									<i class="bi bi-pencil-square"></i> 
									<fmt:message key="settings.factory.manage.label"/>
								</a>
							</div>
						</div>
					</c:forEach>
				</div>
			</div>
		</div>
	</section>
</c:if>
