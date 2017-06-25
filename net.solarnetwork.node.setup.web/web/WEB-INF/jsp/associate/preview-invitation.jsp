<p class="lead"><fmt:message key="preview-invitation.lead"/></p>
<p><fmt:message key="preview-invitation.intro"/></p>

<setup:url value="/associate/verify" var="action"/>
<form:form modelAttribute="details" action="${action}" cssClass="form-horizontal" method="post">
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="invitation-host"><fmt:message key="node.setup.identity.service"/></label>
			<div class="controls" id="invitation-host">
				<span class="uneditable-input span6">${details.host}</span>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="invitation-username"><fmt:message key="node.setup.identity.username"/></label>
			<div class="controls" id="invitation-username">
				<span class="uneditable-input span6">${details.username}</span>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="invitation-identity"><fmt:message key="node.setup.identity.identity"/></label>
			<div class="controls" id="invitation-identity">
				<span class="uneditable-input span6">${details.identityKey}</span>
			</div>
		</div>
		<div class="form-actions">
			<a href="<setup:url value='/associate'/>" class="btn"><fmt:message key='cancel.label'/></a>
			<button type="submit" class="btn btn-primary"><fmt:message key='continue.label'/></button>
		</div>
	</fieldset>
	<sec:csrfInput/>
</form:form>
