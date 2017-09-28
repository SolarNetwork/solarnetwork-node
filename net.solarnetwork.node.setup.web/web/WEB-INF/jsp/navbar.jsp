<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<div class="navbar">
	<div class="navbar-inner">
		<a class="brand" href="<setup:url value='/'/>">
			<img src="<setup:url value='/img/logo-node.svg'/>" alt="<fmt:message key='app.name'/>" width="159" height="30"/>	
		</a>
		<ul class="nav">
			<li ${navloc == 'home' ? 'class="active"' : ''}>
				<sec:authorize access="!hasRole('ROLE_USER')">
					<a href="<setup:url value='/hello'/>"><fmt:message key='link.home'/></a>
				</sec:authorize>
				<sec:authorize access="hasRole('ROLE_USER')">
					<a href="<setup:url value='/a/home.do'/>"><fmt:message key='link.home'/></a>
				</sec:authorize>
			</li>
			<li ${navloc == 'cert' ? 'class="active"' : ''}><a href="<setup:url value='/a/certs'/>"><fmt:message key='link.cert'/></a></li>
			<li ${navloc == 'settings' ? 'class="active"' : ''}><a href="<setup:url value='/a/settings'/>"><fmt:message key='link.settings'/></a></li>
			<li ${navloc == 'controls' ? 'class="active"' : ''}><a href="<setup:url value='/a/controls'/>"><fmt:message key='link.controls'/></a></li>
			<li ${navloc == 'plugins' ? 'class="active"' : ''}><a id="link-plugins" href="<setup:url value='/a/plugins'/>"><fmt:message key='link.plugins'/></a></li>
 		</ul>
		<sec:authorize access="hasRole('ROLE_USER')">
			<ul class="nav pull-right">
				<li class="dropdown">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
						<span class="active-user-display">
							<fmt:message key='nav.label.principal'>
								<fmt:param><sec:authentication property="principal.username" /></fmt:param>
							</fmt:message>
						</span>
						<b class="caret"></b>
					</a>
					<ul class="dropdown-menu">
						<li><a href="<setup:url value='/a/user/change-password'/>"><fmt:message key="link.change-password"/></a></li>
						<li><a href="<setup:url value='/a/user/change-username'/>"><fmt:message key="link.change-username"/></a></li>
						<li><a class="logout" href="#"><fmt:message key='link.logout'/></a></li>
						<c:if test="${not empty systemService}">
							<li role="separator" class="divider"></li>
							<li><a class="restart" href="#"><fmt:message key='link.restart'/></a></li>
						</c:if>
					</ul>
				</li>
			</ul>
			<form id="logout-form" method="post" action="<setup:url value='/logout'/>">
				<sec:csrfInput/>
			</form>
			<form id="restart-modal" class="modal hide fade" action="<setup:url value='/a/home/restart'/>" method="post">
				<div class="modal-header">
					<button type="button" class="close start" data-dismiss="modal">&times;</button>
					<h3><fmt:message key='restart.title'/></h3>
				</div>
				<div class="modal-body start">
					<p><fmt:message key='restart.intro'/></p>
				</div>
				<div class="modal-body success" style="display: none;">
					<div class="progress progress-info progress-striped active">
						<div class="bar" style="width:100%"></div>
					</div>
					<p><fmt:message key='restart.underway'/></p>
				</div>
				<div class="modal-footer start">
					<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
					<button type="submit" class="btn btn-danger reboot"><fmt:message key="restart.action.reboot"/></button>
					<button type="submit" class="btn btn-primary"><fmt:message key="restart.action.restart"/></button>
				</div>
				<input type="hidden" name="reboot" value="false"/>
				<sec:csrfInput/>
			</form>
		</sec:authorize>
	</div>
</div>
