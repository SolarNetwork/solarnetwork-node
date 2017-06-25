<p class="lead"><fmt:message key="new-node.intro"/></p>

<c:set var="myNodesURL" value='${networkLinks["solaruser"]}/u/sec/my-nodes'/>
<p><fmt:message key="node.setup.code.intro">
	<fmt:param value="${myNodesURL}"/>
</fmt:message></p>

<p><fmt:message key="node.setup.restore.option">
	<fmt:param><setup:url value="/associate/restore"/></fmt:param>
</fmt:message></p>

<setup:url value="/associate/preview" var="action"/>
<form:form action="${action}" method="post" cssClass="form-horizontal">
	<form:errors cssClass="alert alert-error" element="div" htmlEscape="false"/>
	<fieldset>
		<c:set var="err"><form:errors path="verificationCode" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty err}'> error</c:if>">
			<label class="control-label" for="${settingId}">
				<fmt:message key="node.setup.code.verificationCode"/>
			</label>
			<div class="controls">
				<fmt:message key='node.setup.code.verificationCode.placeholder' var="placeholder"/>
				<form:textarea path="verificationCode" placeholder="${placeholder}" rows="10" cssClass="span9" required="required"/>
				<c:out value="${err}" escapeXml="false"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<button type="submit" class="btn btn-primary"><fmt:message key='node.setup.code.verify'/></button>
	</div>
	<sec:csrfInput/>
</form:form>
