<section class="intro">
	<p>
		<fmt:message key="controls.manage.intro">
			<fmt:param>${controlId}</fmt:param>
		</fmt:message>
	</p>
</section>
<c:if test="${not empty info}">
<section id="status">
	<h2><fmt:message key="controls.manage.status.title"/></h2>
	<p>
		<fmt:message key="controls.manage.status.intro"/>
	</p>
	<table class="table">
		<tbody>
		<%--
			<tr>
				<td><strong><fmt:message key="controls.info.propertyName.label"/></strong></td>
				<td>
					${info.propertyName}
				</td>
			</tr>
		--%>
			<tr>
				<td><strong><fmt:message key="controls.info.type.label"/></strong></td>
				<td>
					${info.type}
				</td>
			</tr>
			<tr>
				<td><strong><fmt:message key="controls.info.value.label"/></strong></td>
				<td>
					${info.value}
				</td>
			</tr>
		<%--
			<tr>
				<td><strong><fmt:message key="controls.info.unit.label"/></strong></td>
				<td>
					${info.unit}
				</td>
			</tr>
		--%>
			<tr>
				<td><strong><fmt:message key="controls.info.modifiable.label"/></strong></td>
				<td>
					${!info.readonly}
				</td>
			</tr>
		</tbody>
	</table>
</section>
</c:if>
