<p class="lead">
	<fmt:message key="node.setup.identity.intro"><fmt:param value="${association.host}"/></fmt:message>
</p>

<table class="table">
	<tbody>
		<tr><th><fmt:message key="node.setup.identity.service"/></th><td>${association.host}</td></tr>
		<tr><th><fmt:message key="node.setup.identity.identity"/></th><td>${association.identityKey}</td></tr>
		<tr>
			<th><fmt:message key="node.setup.identity.username"/></th>
			<td>${details.username}</td>
		</tr>
		<tr>
			<th><fmt:message key="node.setup.identity.securityPhrase"/></th>
			<td>
				<p>${association.securityPhrase}</p>
				<div class="alert alert-warning"><fmt:message key='node.setup.identity.securityPhrase.caption'/></div>
			</td>
		</tr>
		<tr>
			<th><fmt:message key="node.setup.identity.tos"/></th>
			<td><pre class="pre-scrollable">${association.termsOfService}</pre></td>
		</tr>
	</tbody>
</table>

<p>
	<fmt:message key="node.setup.identity.end"/>
</p>
<c:url value="/associate/confirm" var="action"/>
<form:form action="${action}" method="post" class="form">
	<form:errors cssClass="alert alert-error" element="div"/>
	<a class="btn" href="<c:url value='/associate'/>"><fmt:message key='cancel.label'/></a>
	<button type="submit" class="btn btn-primary" name="confirm"><fmt:message key='node.setup.identity.confirm'/></button>
</form:form>
