<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<div class="navbar">
	<div class="navbar-inner">
		<a class="brand" href="<c:url value='/index.do'/>">
			<img src="<c:url value='/img/logo-node.svg'/>" alt="<fmt:message key='app.name'/>" width="159" height="30"/>	
		</a>
		<ul class="nav">
			<li ${navloc == 'home' ? 'class="active"' : ''}><a href="<c:url value='/hello'/>"><fmt:message key='link.home'/></a></li>
			<li ${navloc == 'cert' ? 'class="active"' : ''}><a href="<c:url value='/a/certs'/>"><fmt:message key='link.cert'/></a></li>
			<li ${navloc == 'settings' ? 'class="active"' : ''}><a href="<c:url value='/a/settings'/>"><fmt:message key='link.settings'/></a></li>
			<li ${navloc == 'controls' ? 'class="active"' : ''}><a href="<c:url value='/a/controls'/>"><fmt:message key='link.controls'/></a></li>
			<li ${navloc == 'plugins' ? 'class="active"' : ''}><a id="link-plugins" href="<c:url value='/a/plugins'/>"><fmt:message key='link.plugins'/></a></li>
 		</ul>
		<sec:authorize ifAnyGranted="ROLE_USER">
			<ul class="nav pull-right">
				<li class="pull-right">
					<a id="link-plugins" href="<c:url value='/logout'/>"><fmt:message key='link.logout'/></a>
				</li>
			</ul>
		</sec:authorize>
	</div>
</div>
