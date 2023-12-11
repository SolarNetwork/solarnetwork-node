<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<p class="lead col-md-9">
	<fmt:message key="loggedoff.intro"/>
</p>

<p class="col-md-9">
	<a href="<setup:url value='/login.do'/>"><fmt:message key="login.label"/></a>
</p>
