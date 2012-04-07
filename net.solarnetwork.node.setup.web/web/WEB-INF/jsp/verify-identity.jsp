<div class="intro">
	<fmt:message key="node.setup.identity.intro"><fmt:param value="${details.hostName}"/></fmt:message>
</div>

<div>
	<table>
		<tr><td><fmt:message key="node.setup.identity.service"/></td><td>${details.hostName}</td></tr>
		<tr><td><fmt:message key="node.setup.identity.identity"/></td><td>${details.identity}</td></tr>
	</table>
</div>
	
<div class="intro"><fmt:message key="node.setup.identity.intro.user"/></div>

<div>
	<table>
		<tr><td><fmt:message key="node.setup.identity.user"/></td><td>${details.userName}</td></tr>
	</table>
</div>

<div class="intro">
<fmt:message key="node.setup.identity.end"/>
	<c:url value="/node/associateNode" var="action"/>
	<form:form action="${action}" method="post">
		<p><form:errors cssClass="error"/></p>
		<input type="submit" value="<fmt:message key='node.setup.identity.confirm'/>" name="confirm"/>
		<input type="submit" value="<fmt:message key='node.setup.identity.cancel'/>" name="cancel"/>
	</form:form>
</div>