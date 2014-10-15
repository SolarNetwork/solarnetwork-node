<p class="lead"><fmt:message key="node.setup.success.intro"/></p>

<table class="table">
	<tr><th><fmt:message key="node.setup.identity.service"/></th><td>${details.host}</td></tr>
	<tr><th><fmt:message key="node.setup.identity.username"/></th><td>${details.username}</td></tr>
	<tr><th><fmt:message key="node.setup.identity.nodeId"/></th><td>${details.networkId}</td></tr>
</table>

<c:set var="myNodesURL" value="http${details.forceTLS or details.port == 443 ? 's' : ''}://${details.host}:${details.port}/solaruser/u/sec/my-nodes"/>
<c:choose>
	<c:when test="${empty details.networkCertificateStatus}">
		<p>
			<fmt:message key="node.setup.success.visit">
				<fmt:param value="${myNodesURL}"/>
				<fmt:param><c:url value='/certs'/></fmt:param>
			</fmt:message>
		</p>
	</c:when>
	<c:when test="${details.networkCertificateStatus == 'Active'}">
		<p>
			<fmt:message key="node.setup.success.active">
				<fmt:param value="${myNodesURL}"/>
			</fmt:message>
		</p>
	</c:when>
	<c:when test="${details.networkCertificateStatus == 'Requested'}">
		<p>
			<fmt:message key="node.setup.success.requested">
				<fmt:param value="${myNodesURL}"/>
				<fmt:param><c:url value='/certs'/></fmt:param>
			</fmt:message>
		</p>
	</c:when>
	<c:when test="${not empty csr}">
		<h2><fmt:message key='certs.csr.title'/></h2>
		<p><fmt:message key='certs.csr.intro'/></p>
		<pre class="cert well">${csr}</pre>
	</c:when>
</c:choose>

