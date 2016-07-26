<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<p class="lead">
	<fmt:message key='home.intro-loggedin'>
		<fmt:param><sec:authentication property="principal.username" /></fmt:param>
	</fmt:message>
</p>
