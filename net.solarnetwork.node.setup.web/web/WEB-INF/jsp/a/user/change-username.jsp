<div class="hidden alert alert-success" id="change-username-success">
	<fmt:message key='user.changeUsername.success'/>
</div>

<section class="intro">
	<p>
		<fmt:message key="user.changeUsername.intro"/>
	</p>
</section>

<setup:url value="/a/user/change-username" var="action"/>
<form:form commandName="user" cssClass="form-horizontal" id="change-username-form" action="${action}" method="post" >
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="old-username"><fmt:message key="user.username.label"/></label>
			<div class="controls" id="old-username">
				<span class="input-large uneditable-input active-user-display"><sec:authentication property="principal.username" /></span>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="login-username"><fmt:message key="user.newUsername.label"/></label>
			<div class="controls">
				<input type="text" class="input-large" name="username" id="login-username" maxlength="255" required="required"/>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="login-username-again"><fmt:message key="user.newUsernameAgain.label"/></label>
			<div class="controls">
				<input type="text" class="input-large" name="usernameAgain" id="login-username-again" maxlength="255" required="required"/>
			</div>
		</div>
	</fieldset>
	<div class="control-group">
		<div class="controls">
			<button type="submit" class="btn btn-primary"><fmt:message key='user.action.changeUsername'/></button>
		</div>
	</div>
	<sec:csrfInput/>
</form:form>
