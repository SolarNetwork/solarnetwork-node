<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<p class="lead span9">
	<fmt:message key="loggedoff.intro"/>
</p>

<p class="span9">
	<a href="<setup:url value='/login.do'/>"><fmt:message key="login.label"/></a>
</p>
