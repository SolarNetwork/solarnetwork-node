<c:if test="${fn:length(controlIds) < 1}">
	<p><fmt:message key="controls.none.message"/>
</c:if>
<c:if test="${fn:length(controlIds) > 0}">
	<section class="intro">
		<p>
			<fmt:message key="controls.intro"/>
		</p>
	</section>
	<section id="providers">
		<div class="row setting-components gap-3">
			<c:forEach items="${controlIds}" var="controlId">
				<!--  ${controlId} -->
				<div class="row justify-content-between align-items-center">
					<div class="col">
						<strong>${controlId}</strong>
					</div>
					<div class="col-auto">
						<a class="btn btn-light" href="<setup:url value='/a/controls/manage?id=${controlId}'/>">
							<i class="far fa-pen-to-square"></i> 
							<span><fmt:message key="controls.manage.label"/></span>
						</a>
					</div>
				</div>
			</c:forEach>
		</div>
	</section>
</c:if>
