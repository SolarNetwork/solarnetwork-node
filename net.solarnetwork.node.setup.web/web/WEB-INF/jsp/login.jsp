<div class="content-absolute container">
	<div class="row justify-content-center content-vertical-center">
		<div class="col-sm-10 col-md-8 col-lg-6">	
			<c:if test="${not empty param.login_error}">
				<p class="global-error alert alert-danger">
					<fmt:message key="login.error"/>
					<!--
					<c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>
					-->
				</p>
			</c:if>
	
			<p class="intro">
				<fmt:message key="login.intro">
					<fmt:param><setup:url value="/associate"/></fmt:param>
				</fmt:message>
			</p>
			
			<form name="f" action="<setup:url value='/login'/>" class="login-form col" method="post">
				<fieldset class="row gy-3">
					<div class="col-12">
						<label for="login-username"><fmt:message key="user.username.label"/></label>
						<input class="form-control" type="text" name="username" id="login-username" maxlength="240" 
							value="<c:if test='${not empty param.login_error}'>${SPRING_SECURITY_LAST_USERNAME}</c:if>"/>
					</div>
					<div class="col-12">
						<label class="control-label" for="login-password"><fmt:message key="user.password.label"/></label>
						<input class="form-control" type="password" name="password" id="login-password" maxlength="255" />
					</div>
					<div class="d-grid">
						<button type="submit" class="btn btn-primary"><fmt:message key='login.label'/></button>
					</div>		
				</fieldset>
				<sec:csrfInput/>
			</form>
		</div>
	</div>
</div>