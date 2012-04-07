<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<body>
	<p>You're logged in as <sec:authentication property="principal.username" />.</p>
</body>
