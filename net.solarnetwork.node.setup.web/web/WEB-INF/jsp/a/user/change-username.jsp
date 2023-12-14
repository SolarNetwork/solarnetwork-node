<div class="row justify-content-center">
	<div class="col-md-6">		
		<div class="hidden alert alert-success" id="change-username-success">
			<fmt:message key='user.changeUsername.success'/>
		</div>
		
		<section class="intro">
			<p><fmt:message key="user.changeUsername.intro"/></p>
		</section>
		
		<setup:url value="/a/user/change-username" var="action"/>
		<form:form modelAttribute="user" id="change-username-form" action="${action}" method="post" >
			<fieldset class="row gy-3">
				<div class="col-12">
					<label class="form-label" for="old-username"><fmt:message key="user.username.label"/></label>
					<input class="form-control" type="text" id="old-username" readonly value="<sec:authentication property='principal.username'/>">
				</div>
				<div class="col-12">
					<label class="form-label" for="login-username"><fmt:message key="user.newUsername.label"/></label>
					<input class="form-control" type="text" name="username" id="login-username" maxlength="255" required="required"/>
				</div>
				<div class="col-12">
					<label class="form-label" for="login-username-again"><fmt:message key="user.newUsernameAgain.label"/></label>
					<input class="form-control" type="text" name="usernameAgain" id="login-username-again" maxlength="255" required="required"/>
				</div>
				<div class="col-12">
					<button type="submit" class="btn btn-primary"><fmt:message key='user.action.changeUsername'/></button>
				</div>
			</fieldset>
			<sec:csrfInput/>
		</form:form>
	</div>
</div>
