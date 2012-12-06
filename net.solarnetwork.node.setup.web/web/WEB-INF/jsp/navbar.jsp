<div class="navbar">
	<div class="navbar-inner">
		<a class="brand" href="<c:url value='/index.do'/>">
			<img src="<c:url value='/img/logo-node.svg'/>" alt="<fmt:message key='app.name'/>" width="159" height="30"/>	
		</a>
		<ul class="nav">
			<li ${navloc == 'home' ? 'class="active"' : ''}><a href="<c:url value='/hello'/>"><fmt:message key='link.home'/></a></li>
			<li ${navloc == 'cert' ? 'class="active"' : ''}><a href="<c:url value='/certs'/>"><fmt:message key='link.cert'/></a></li>
			<li ${navloc == 'settings' ? 'class="active"' : ''}><a href="<c:url value='/settings'/>"><fmt:message key='link.settings'/></a></li>
 		</ul>
	</div>
</div>
