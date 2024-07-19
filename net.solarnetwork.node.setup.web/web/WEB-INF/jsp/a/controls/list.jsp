<c:if test="${fn:length(controlIds) < 1}">
	<p><fmt:message key="controls.none.message"/>
</c:if>
<c:if test="${fn:length(controlIds) > 0}">
	<section class="intro">
		<h2><fmt:message key="controls.title"/></h2>	
		<p><fmt:message key="controls.intro"/></p>
	</section>
	<section id="providers">
		<div class="container">
			<div class="row setting-components">
				<div class="col">
					<c:forEach items="${controlIds}" var="controlId">
						<!--  ${controlId} -->
						<div class="row my-3 justify-content-between align-items-center">
							<div class="col">
								<strong>${controlId}</strong>
							</div>
							<div class="col-auto">
								<a class="btn btn-light" href="<setup:url value='/a/controls/manage?id=${controlId}'/>">
									<i class="bi bi-pencil-square"></i> 
									<span><fmt:message key="controls.manage.label"/></span>
								</a>
							</div>
						</div>
					</c:forEach>
				</div>
			</div>
		</div>
	</section>
</c:if>
