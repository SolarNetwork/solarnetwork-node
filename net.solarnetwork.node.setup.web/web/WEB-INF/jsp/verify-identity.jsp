<p>
	<fmt:message key="node.setup.identity.intro"><fmt:param value="${details.host}"/></fmt:message>
</p>

<table class="table">
	<tbody>
		<tr><th><fmt:message key="node.setup.identity.service"/></th><td>${details.host}</td></tr>
		<tr><th><fmt:message key="node.setup.identity.identity"/></th><td>${details.identityKey}</td></tr>
	</tbody>
</table>

<p><fmt:message key="node.setup.identity.intro.user"/></p>

<table class="table">
	<tbody>
		<tr>
			<th><fmt:message key="node.setup.identity.user"/></th>
			<td>${details.username}</td>
		</tr>
		<tr>
			<th><fmt:message key="node.setup.identity.securityPhrase"/></th>
			<td>
				<p>${details.securityPhrase}</p>
				<div class="alert alert-warning"><fmt:message key='node.setup.identity.securityPhrase.caption'/></div>
			</td>
		</tr>
	</tbody>
</table>

<p>
	<fmt:message key="node.setup.identity.end"/>
</p>
<c:url value="/associate/confirm" var="action"/>
<form:form action="${action}" method="post" class="form">
	<form:errors cssClass="alert alert-error" element="div"/>
	<button type="submit"  class="btn" name="cancel"><fmt:message key='node.setup.identity.cancel'/></button>
	<button type="submit" class="btn btn-primary" name="confirm"><fmt:message key='node.setup.identity.confirm'/></button>
</form:form>
