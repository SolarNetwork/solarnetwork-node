<div class="row justify-content-center">
	<div class="col-md-6">		
		<div class="hidden alert alert-success" id="change-password-success">
			<fmt:message key='user.changePassword.success'/>
		</div>
		
		<section class="intro">
			<p><fmt:message key="user.changePassword.intro"/></p>
		</section>
		
		<setup:url value="/a/user/change-password" var="action"/>
		<form:form modelAttribute="user" cssClass="form-horizontal" id="change-password-form" action="${action}" method="post">
			<fieldset class="row gy-3">
				<div class="col-12">
					<label class="form-label" for="old-password"><fmt:message key="user.oldPassword.label"/></label>
					<form:password cssClass="form-control" path="oldPassword" showPassword="true" id="old-password" maxlength="255" required="required" />
				</div>
				<div class="col-12">
					<label class="form-label" for="login-password"><fmt:message key="user.newPassword.label"/></label>
					<input class="form-control" type="password" name="password" id="login-password" maxlength="255" required="required"/>
				</div>
				<div class="col-12">
					<label class="form-label" for="login-password-again"><fmt:message key="user.newPasswordAgain.label"/></label>
					<input class="form-control" type="password" name="passwordAgain" id="login-password-again" maxlength="255" required="required"/>
				</div>
				<div class="col-12">
					<button type="submit" class="btn btn-primary"><fmt:message key='user.action.changePassword'/></button>
				</div>
			</fieldset>
			<sec:csrfInput/>
		</form:form>
	</div>
</div>