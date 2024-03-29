<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<p class="lead"><fmt:message key="node.setup.success.intro"/></p>

<table class="table">
	<tr><th><fmt:message key="node.setup.identity.service"/></th><td>${details.host}</td></tr>
	<tr><th><fmt:message key="node.setup.identity.nodeId"/></th><td>${details.networkId}</td></tr>
	<tr><th><fmt:message key="node.setup.identity.username"/></th><td>${details.username}</td></tr>
	<c:if test="${not empty user}">
		<tr>
			<th><fmt:message key="node.setup.success.user.password"/></th>
			<td>
				<p class="text-danger"><code>${user.password}</code></p>
				<div class="alert alert-warning">
					<fmt:message key='node.setup.success.user.intro'>
						<fmt:param><setup:url value="/a/user/change-password">
							<spring:param name="old" value="${user.password}"/>
						</setup:url></fmt:param>
					</fmt:message>
				</div>
			</td>
		</tr>
	</c:if>
</table>

<section class="my-3">
	<c:set var="myNodesURL" value="${association.solarUserServiceURL}/u/sec/my-nodes"/>
	<c:choose>
		<c:when test="${empty details.networkCertificateStatus}">
			<p>
				<fmt:message key="node.setup.success.visit">
					<fmt:param value="${myNodesURL}"/>
					<fmt:param><setup:url value='/a/certs'/></fmt:param>
				</fmt:message>
			</p>
		</c:when>
		<c:when test="${details.networkCertificateStatus == 'Active'}">
			<p>
				<fmt:message key="node.setup.success.active">
					<fmt:param value="${myNodesURL}"/>
				</fmt:message>
			</p>
			<div class="d-grid">
				<a class="btn btn-primary" href="<setup:url value='/a/home'/>">
					<fmt:message key="node.setup.success.gohome"/>
				</a>
			</div>
		</c:when>
		<c:when test="${details.networkCertificateStatus == 'Requested'}">
			<p>
				<fmt:message key="node.setup.success.requested">
					<fmt:param value="${myNodesURL}"/>
					<fmt:param><setup:url value='/a/certs'/></fmt:param>
				</fmt:message>
			</p>
		</c:when>
		<c:when test="${not empty csr}">
			<h2><fmt:message key='certs.csr.title'/></h2>
			<p><fmt:message key='certs.csr.intro'/></p>
			<pre class="cert alert alert-info">${csr}</pre>
		</c:when>
	</c:choose>
</section>