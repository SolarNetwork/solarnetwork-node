<body>
	<c:if test="${not empty param.login_error}">
		<div class="global-error">
			<fmt:message key="login.error"/>
		</div>
	</c:if>
	
	<div class="intro">
		<fmt:message key="new-node.intro"/>
	</div>
	
	<c:url value="/hello/chooseNetwork" var="action"/>
	<form:form modelAttribute="command" action="${action}" cssClass="new-node-form" method="post">
		<fieldset>
			<div class="field input">
				<label for="network-host"><fmt:message key="network.host.label"/></label>
				<div>
					<form:input path="hostName" id="network-host" maxlength="255"/>
					<div class="caption"><fmt:message key="network.host.caption"/></div>
				</div>
			</div>
			<div class="field input">
				<label for="network-port"><fmt:message key="network.port.label"/></label>
				<div>
					<form:input path="hostPort" id="network-port" maxlength="8"/>
					<div class="caption"><fmt:message key="network.port.caption"/></div>
				</div>
			</div>
			<div class="button-group">
				<input name="submit" type="submit" value="<fmt:message key='choose.network.host.label'/>"/>
			</div>
		</fieldset>
	</form:form>

</body>
