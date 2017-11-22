<head>
	<title><fmt:message key="app.name"/></title>
	<meta name="viewport" content="width=device-width, initial-scale=1.0" />
	<meta name="csrf" content="${_csrf.token}"/>
	<meta name="csrf_header" content="${_csrf.headerName}"/>
	<sec:authorize access="hasRole('ROLE_USER')">
		<meta name="authenticated" content="true"/>
		<meta name="nodeId" content="${identityService.nodeId}"/>
	</sec:authorize>	
	<c:import url="/WEB-INF/jsp/default-head-resources.jsp"/>
</head>
