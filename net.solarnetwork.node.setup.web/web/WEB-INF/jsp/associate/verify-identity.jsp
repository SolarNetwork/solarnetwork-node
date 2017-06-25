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
<setup:url value="/associate/confirm" var="action"/>
<form:form action="${action}" method="post" class="form-horizontal" id="associate-confirm-form">
	<fieldset>
		<form:errors cssClass="alert alert-error" element="div"/>
		<div class="control-group">
			<label class="control-label" for="invitation-certpass"><fmt:message key="node.setup.associate.certpass"/></label>
			<div class="controls">
				<input type="password" name="keystorePassword" maxlength="64" id="invitation-certpass"/>
				<span class="help-block"><fmt:message key="node.setup.associate.certpass.caption"/></span>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="invitation-certpass-again"><fmt:message key="node.setup.associate.certpass.again"/></label>
			<div class="controls">
				<input type="password" name="keystorePasswordAgain" maxlength="64" id="invitation-certpass-again"
					data-tooshort="<fmt:message key='node.setup.associate.certpass.tooshort'/>"
					data-mismatch="<fmt:message key='node.setup.associate.certpass.mismatch'/>"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<a class="btn" href="<setup:url value='/associate'/>"><fmt:message key='cancel.label'/></a>
		<button type="submit" class="btn btn-primary" name="confirm"><fmt:message key='node.setup.identity.confirm'/></button>
		<div class="alert alert-error hidden" id="invitation-certpass-reiterate">
	 		<strong><fmt:message key='node.setup.associate.certpass.reiterate.title'/></strong>
	 		<fmt:message key='node.setup.associate.certpass.reiterate'/>
	 	</div>
	</div>
	<sec:csrfInput/>
</form:form>
