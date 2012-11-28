<p class="intro"><fmt:message key="node.setup.code.intro"/></p>

<c:url value="/associate/verify" var="action"/>
<form:form action="${action}" method="post" cssClass="form-horizontal">
	<form:errors cssClass="alert alert-error" element="div"/>
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="${settingId}">
				<fmt:message key="node.setup.code.verificationCode"/>
			</label>
			<div class="controls">
				<fmt:message key='node.setup.code.verificationCode.placeholder' var="placeholder"/>
				<form:textarea path="verificationCode" placeholder="${placeholder}" rows="10" cssClass="span9"/>
				<form:errors path="verificationCode" cssClass="help-inline error" element="span"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<button type="submit" class="btn btn-primary"><fmt:message key='node.setup.code.verify'/></button>
	</div>
</form:form>
