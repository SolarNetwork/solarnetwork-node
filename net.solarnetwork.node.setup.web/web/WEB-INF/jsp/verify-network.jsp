<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<body>
	<div class="intro">
		<fmt:message key="verify-network.intro"/>
	</div>
	
	<c:url value="/hello/acceptNetwork" var="action"/>
	<form:form modelAttribute="details" action="${action}" cssClass="verify-network-form" method="post">
		<fieldset>
			<div class="field">
				<label for="network.host"><fmt:message key="network.host.label"/></label>
				<div>
					${details.hostName} ${details.hostPort}
				</div>
			</div>
			<div class="field">
				<label for="network.identity"><fmt:message key="network.identity.label"/></label>
				<div id="network.identity">
					${details.identity}
				</div>
			</div>
			<div class="field">
				<label for="network.tos"><fmt:message key="network.tos.label"/></label>
				<div id="network.tos">
					${details.tos}
				</div>
			</div>
			<div class="button-group">
				<p>
					<fmt:message key="accept.network.intro"/>
				</p>
				<input name="submit" type="submit" value="<fmt:message key='accept.network.label'/>"/>
			</div>
		</fieldset>
	</form:form>

</body>
