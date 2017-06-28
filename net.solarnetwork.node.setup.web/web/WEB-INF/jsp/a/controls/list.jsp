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
		<table class="table">
			<tbody>
			<c:forEach items="${controlIds}" var="controlId">
				<tr>
					<td><strong>${controlId}</strong></td>
					<td>
						<a class="btn" href="<setup:url value='/a/controls/manage?id=${controlId}'/>">
							<i class="icon-edit icon-large"></i> 
							<fmt:message key="controls.manage.label"/>
						</a>
					</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>
