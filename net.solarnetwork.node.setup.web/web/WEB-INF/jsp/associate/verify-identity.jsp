<p class="lead">
	<fmt:message key="node.setup.identity.intro"><fmt:param value="${association.host}"/></fmt:message>
</p>

<table class="table">
	<tbody>
		<tr><th><fmt:message key="node.setup.identity.service"/></th><td>${association.host}</td></tr>
		<tr><th><fmt:message key="node.setup.identity.identity"/></th><td><c:out value="${association.identityKey}"/></td></tr>
		<tr>
			<th><fmt:message key="node.setup.identity.username"/></th>
			<td><c:out value="${details.username}"/></td>
		</tr>
		<tr>
			<th><fmt:message key="node.setup.identity.securityPhrase"/></th>
			<td>
				<p><c:out value="${association.securityPhrase}"/></p>
				<div class="alert alert-warning"><fmt:message key='node.setup.identity.securityPhrase.caption'/></div>
			</td>
		</tr>
		<tr>
			<th><fmt:message key="node.setup.identity.tos"/></th>
			<td><div class="overflow-y-auto" style="max-height: 12rem;"><c:out value="${association.termsOfService}"/></div></td>
		</tr>
	</tbody>
</table>

<p>
	<fmt:message key="node.setup.identity.end"/>
</p>
<setup:url value="/associate/confirm" var="action"/>
<form:form action="${action}" method="post" id="associate-confirm-form" cssClass="my-3">
	<fieldset>
		<form:errors cssClass="alert alert-danger" element="div"/>
		<div class="row">
			<label class="col-sm-4 col-md-3 col-form-label" for="invitation-certpass"><fmt:message key="node.setup.associate.certpass"/></label>
			<div class="col-sm-8 col-md-9">
				<input class="form-control" type="password" name="keystorePassword" maxlength="64" id="invitation-certpass" aria-describedby="cert-password-help">
				<div class="form-text" id="cert-password-help"><fmt:message key="node.setup.associate.certpass.caption"/></div>
			</div>
		</div>
		<div class="row mt-3">
			<label class="col-sm-4 col-md-3 col-form-label" for="invitation-certpass-again"><fmt:message key="node.setup.associate.certpass.again"/></label>
			<div class="col-sm-8 col-md-9">
				<input class="form-control" type="password" name="keystorePasswordAgain" maxlength="64" id="invitation-certpass-again"
					data-tooshort="<fmt:message key='node.setup.associate.certpass.tooshort'/>"
					data-mismatch="<fmt:message key='node.setup.associate.certpass.mismatch'/>">
			</div>
		</div>
	</fieldset>
	<div class="row mt-5">
		<div class="col offset-sm-4 offset-md-3">
			<div class="alert alert-danger hidden" id="invitation-certpass-reiterate">
		 		<strong><fmt:message key='node.setup.associate.certpass.reiterate.title'/></strong>
		 		<fmt:message key='node.setup.associate.certpass.reiterate'/>
		 	</div>
			<a class="btn btn-secondary" href="<setup:url value='/associate'/>"><fmt:message key='cancel.label'/></a>
			<button type="submit" class="btn btn-primary" name="confirm"><fmt:message key='node.setup.identity.confirm'/></button>
		</div>
	</div>
	<sec:csrfInput/>
</form:form>
