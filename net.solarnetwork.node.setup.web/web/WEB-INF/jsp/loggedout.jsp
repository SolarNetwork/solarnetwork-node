<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<body>
	<div class="intro">
		<fmt:message key="loggedoff.intro"/>
	</div>
	<div>
		<a href="<c:url value='/login.do'/>"><fmt:message key="login.label"/></a>
	</div>
</body>