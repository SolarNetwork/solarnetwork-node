<c:if test="${fn:length(providerIds) < 1}">
	<p><fmt:message key="controls.providers.none.message"/>
</c:if>
<c:if test="${fn:length(providerIds) > 0}">
	<section class="intro">
		<p>
			<fmt:message key="controls.intro"/>
		</p>
	</section>
	<section id="providers">
		<table class="table">
			<tbody>
			<c:forEach items="${providerIds}" var="providerId" varStatus="providerIdStatus">
				<tr>
					<td><strong>${providerId}</strong></td>
					<td>
						<a class="btn" href="<c:url value='/controls/manage?id=${providerId}'/>">
							<i class="icon-edit icon-large"></i> 
							<fmt:message key="controls.provider.manage.label"/>
						</a>
					</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>
