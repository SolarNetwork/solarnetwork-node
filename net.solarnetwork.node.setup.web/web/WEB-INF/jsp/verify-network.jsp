<div class="intro">
	<fmt:message key="verify-network.intro"/>
</div>

<c:url value="/associate/acceptNetwork" var="action"/>
<form:form modelAttribute="details" action="${action}" cssClass="form-horizontal" method="post">
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="network-host"><fmt:message key="network.host.label"/></label>
			<div class="controls" id="network-host">
				${details.hostName} ${details.hostPort}
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="network-identity"><fmt:message key="network.identity.label"/></label>
			<div class="controls" id="network-identity">
				${details.identity}
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="network-tos"><fmt:message key="network.tos.label"/></label>
			<div class="controls" id="network-tos">
				${details.tos}
			</div>
		</div>
		<div class="form-actions">
			<p><fmt:message key="accept.network.intro"/></p>
			<button type="submit"><fmt:message key='accept.network.label'/></button>
		</div>
	</fieldset>
</form:form>
