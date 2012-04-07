<div class="intro"><fmt:message key="node.setup.code.intro"/></div>

<div>
	
	<c:url value="/node/verifyCode" var="action"/>
	<form:form action="${action}" method="post">
		<p><form:errors cssClass="error"/></p>
		
		<div style="float:left"><fmt:message key="node.setup.code.verificationCode"/> <form:errors path="verificationCode" cssClass="error"/></div>
	
		<fmt:message key='node.setup.code.verificationCode.placeholder' var="placeholder"/>
		<form:textarea path="verificationCode" placeholder="${placeholder}" rows="10" cols="100"/>
		<input type="submit" value="<fmt:message key='node.setup.code.verify'/>" />
	</form:form>
	
</div>