<section id="factories">
	<h2>
		<a id="components-section" href="#components-section"
			class="anchor" aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
		<fmt:message key="settings.factories.title"/>
	</h2>
	<c:choose>
		<c:when test="${fn:length(factories) > 0}">
			<p><fmt:message key="settings.factories.intro"/></p>
			<div class="container">
				<div class="row setting-components">
					<div class="col">
						<c:forEach items="${factories}" var="factory" varStatus="factoryStatus">
							<div class="row my-3 justify-content-between align-items-center">
								<!--  ${factory.factoryUid} -->
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
									<a class="btn btn-light" href="<setup:url value='/a/settings/manage?uid=${factory.factoryUid}'/>">
										<i class="bi bi-pencil-square"></i>
										<fmt:message key="settings.factory.manage.label"/>
									</a>
								</div>
							</div>
						</c:forEach>
					</div>
				</div>
			</div>
		</c:when>
		<c:otherwise>
			<p><fmt:message key="settings.factories.none"/></p>
		</c:otherwise>
	</c:choose>
</section>
