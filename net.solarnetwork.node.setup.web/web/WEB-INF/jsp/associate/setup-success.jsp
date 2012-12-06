<p class="lead"><fmt:message key="node.setup.success.intro"/></p>

<table class="table">
	<tr><th><fmt:message key="node.setup.identity.service"/></th><td>${details.host}</td></tr>
	<tr><th><fmt:message key="node.setup.identity.username"/></th><td>${details.username}</td></tr>
	<tr><th><fmt:message key="node.setup.identity.nodeId"/></th><td>${details.networkId}</td></tr>
</table>

<p>
	<fmt:message key="node.setup.success.visit">
		<fmt:param value="http${details.forceTLS or details.port == 443 ? 's' : ''}://${details.host}:${details.port}/solarreg/u/sec/my-nodes"/>
	</fmt:message>
</p>

<c:if test="${not empty csr}">
	<h2><fmt:message key='node.setup.success.csr.title'/></h2>
	<p><fmt:message key='node.setup.success.csr.intro'/></p>
	<pre class="cert well">${csr}</pre>
</c:if>
