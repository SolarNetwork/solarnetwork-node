<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<body>
	<c:if test="${not empty param.login_error}">
		<div class="global-error">
			<fmt:message key="login.error"/>
			<!--
			<c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>
			-->
		</div>
	</c:if>
	
	<div class="intro">
		<fmt:message key="login.intro">
			<fmt:param><c:url value="/register.do"/></fmt:param>
		</fmt:message>
	</div>
	
	<form name="f" action="<c:url value='/j_spring_security_check'/>" 
		class="login-form" method="post">
		<fieldset>
			<div class="field input">
				<label for="username"><fmt:message key="user.email.label"/></label>
				<div>
					<input type="text" name="j_username" id="username" maxlength="240" 
						value="<c:if test='${not empty param.login_error}'><c:out value='${SPRING_SECURITY_LAST_USERNAME}'/></c:if>"/>
				</div>
			</div>
			<div class="field input">
				<label for="user.password"><fmt:message key="user.password.label"/></label>
				<div>
					<input type="password" name="j_password" id="password" maxlength="255"/>
				</div>
			</div>
			<div class="button-group">
				<input name="submit" type="submit" value="<fmt:message key='login.label'/>"/>
			</div>
		</fieldset>
	</form>
</body>
