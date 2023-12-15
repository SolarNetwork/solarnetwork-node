<p class="lead"><fmt:message key="preview-invitation.lead"/></p>
<p><fmt:message key="preview-invitation.intro"/></p>

<setup:url value="/associate/verify" var="action"/>
<form:form modelAttribute="details" action="${action}" method="post">
	<fieldset>
		<div class="row">
			<label class="col-sm-4 col-md-3 col-form-label" for="invitation-host"><fmt:message key="node.setup.identity.service"/></label>
			<div class="col-sm-7 col-md-8">
				<div class="plaintext-form-value" id="invitation-host">${details.host}</div>
			</div>
		</div>
		<div class="row mt-3">
			<label class="col-sm-4 col-md-3 col-form-label" for="invitation-username"><fmt:message key="node.setup.identity.username"/></label>
			<div class="col-sm-7 col-md-8">
				<div class="plaintext-form-value" id="invitation-username">${details.username}</div>
			</div>
		</div>
		<div class="row mt-3">
			<label class="col-sm-4 col-md-3 col-form-label" for="invitation-identity"><fmt:message key="node.setup.identity.identity"/></label>
			<div class="col-sm-7 col-md-8">
				<div class="plaintext-form-value" id="invitation-identity">
					<c:out value="${details.identityKey}" escapeXml="false"/>
				</div>
			</div>
		</div>
		<div class="row mt-5">
			<div class="col offset-sm-4 offset-md-3">
				<a href="<setup:url value='/associate'/>" class="btn btn-secondary"><fmt:message key='cancel.label'/></a>
				<button type="submit" class="btn btn-primary"><fmt:message key='continue.label'/></button>
			</div>
		</div>
	</fieldset>
	<sec:csrfInput/>
</form:form>
