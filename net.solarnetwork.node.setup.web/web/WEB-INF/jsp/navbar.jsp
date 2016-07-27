<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<div class="navbar">
	<div class="navbar-inner">
		<a class="brand" href="<c:url value='/index.do'/>">
			<img src="<c:url value='/img/logo-node.svg'/>" alt="<fmt:message key='app.name'/>" width="159" height="30"/>	
		</a>
		<ul class="nav">
			<li ${navloc == 'home' ? 'class="active"' : ''}>
				<sec:authorize ifNotGranted="ROLE_USER">
					<a href="<c:url value='/hello'/>"><fmt:message key='link.home'/></a>
				</sec:authorize>
				<sec:authorize ifAnyGranted="ROLE_USER">
					<a href="<c:url value='/a/home.do'/>"><fmt:message key='link.home'/></a>
				</sec:authorize>
			</li>
			<li ${navloc == 'cert' ? 'class="active"' : ''}><a href="<c:url value='/a/certs'/>"><fmt:message key='link.cert'/></a></li>
			<li ${navloc == 'settings' ? 'class="active"' : ''}><a href="<c:url value='/a/settings'/>"><fmt:message key='link.settings'/></a></li>
			<li ${navloc == 'controls' ? 'class="active"' : ''}><a href="<c:url value='/a/controls'/>"><fmt:message key='link.controls'/></a></li>
			<li ${navloc == 'plugins' ? 'class="active"' : ''}><a id="link-plugins" href="<c:url value='/a/plugins'/>"><fmt:message key='link.plugins'/></a></li>
 		</ul>
		<sec:authorize ifAnyGranted="ROLE_USER">
			<ul class="nav pull-right">
				<li class="dropdown">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
						<fmt:message key='nav.label.principal'>
							<fmt:param><sec:authentication property="principal.username" /></fmt:param>
						</fmt:message>
						<b class="caret"></b>
					</a>
					<ul class="dropdown-menu">
						<li  ${navloc == 'profile' ? 'class="active"' : ''}>
							<a href="<c:url value='/a/user/change-password'/>"><fmt:message key="link.change-password"/></a>
						</li>
						<li><a href="<c:url value='/logout'/>"><fmt:message key='link.logout'/></a></li>
					</ul>
				</li>
			</ul>
		</sec:authorize>
	</div>
</div>
