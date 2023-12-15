<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<sec:authorize access="isAuthenticated()" var="isLoggedIn"/>
<div class="navbar navbar-expand-md bg-body-tertiary mb-3">
	<div class="container-fluid">
		<a class="navbar-brand" href="<setup:url value='${isLoggedIn ? "/a/home" : "/"}'/>">
			<img src="<setup:url value='/img/logo-node.svg'/>" alt="<fmt:message key='app.name'/>" width="143" height="28"/>
		</a>
		<c:if test="${needsAssociation != true}">
			<button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbar-content" aria-controls="navbar-content" aria-expanded="false" aria-label="<fmt:message key='nav.toggle.title'/>">
				<span class="navbar-toggler-icon"></span>
			</button>
			<div class="collapse navbar-collapse flex-grow-0" id="navbar-content">		
				<ul class="navbar-nav gap-3">
					<li class="nav-item">
						<a class="nav-link${navloc == 'home' and isLoggedIn ? ' active' : ''}" href="<setup:url value='/a/home'/>">
							<c:choose>
								<c:when test="${isLoggedIn}">
									<fmt:message key='link.home'/>
								</c:when>
								<c:otherwise>
									<fmt:message key='link.login'/>
								</c:otherwise>
							</c:choose>
						</a>
					</li>
		
					<sec:authorize access="hasRole('ROLE_USER')">
						<li class="nav-item dropdown">
							<a href="#" class="nav-link dropdown-toggle${navloc == 'settings'
								or navloc == 'settings-component'
								or navloc == 'filters'
								or navloc == 'filters-component'
								or navloc == 'opmodes' ? ' active' : ''}" data-bs-toggle="dropdown" role="button" aria-expanded="false">
								<fmt:message key='link.settings'/>
								<b class="caret"></b>
							</a>
							<ul class="dropdown-menu">
								<li><a class="dropdown-item${navloc == 'settings' ? ' active' : ''}" href="<setup:url value='/a/settings'/>"><fmt:message key='link.settings'/></a></li>
								<li><a class="dropdown-item${navloc == 'filters' ? ' active' : ''}" href="<setup:url value='/a/settings/filters'/>"><fmt:message key='link.filters'/></a></li>
								<li><a class="dropdown-item${navloc == 'logging' ? ' active' : ''}" href="<setup:url value='/a/logging'/>"><fmt:message key='link.logging'/></a></li>
								<li><a class="dropdown-item${navloc == 'opmodes' ? ' active' : ''}" href="<setup:url value='/a/opmodes'/>"><fmt:message key='link.opmodes'/></a></li>
							</ul>
						</li>
		
						<li class="nav-item dropdown">
							<a href="#" class="nav-link dropdown-toggle${navloc == 'cert' or navloc == 'plugins' or navloc == 'packages' ? ' active' : ''}" data-bs-toggle="dropdown" role="button" aria-expanded="false">
								<fmt:message key='link.system'/>
								<b class="caret"></b>
							</a>
							<ul class="dropdown-menu">
								<li><a class="dropdown-item${navloc == 'cert' ? ' active' : ''}" href="<setup:url value='/a/certs'/>"><fmt:message key='link.cert'/></a></li>
								<c:if test="${not empty platformPackageService}">
									<li><a class="dropdown-item${navloc == 'packages' ? ' active' : ''}" id="link-packages" href="<setup:url value='/a/packages'/>"><fmt:message key='link.packages'/></a></li>
								</c:if>
								<li><a class="dropdown-item${navloc == 'plugins' ? ' active' : ''}" id="link-plugins" href="<setup:url value='/a/plugins'/>"><fmt:message key='link.plugins'/></a></li>
								<c:if test="${not empty systemService}">
									<li role="separator"><hr class="dropdown-divider"></li>
									<li><a class="dropdown-item${navloc == 'hosts' ? ' active' : ''}" href="<setup:url value='/a/hosts'/>"><fmt:message key='link.hosts'/></a></li>
									<li role="separator"><hr class="dropdown-divider"></li>
									<li><button class="dropdown-item" data-bs-toggle="modal" data-bs-target="#restart-modal"><fmt:message key='link.restart'/></button></li>
									<li role="separator"><hr class="dropdown-divider"></li>
									<li><button class="dropdown-item" data-bs-toggle="modal" data-bs-target="#reset-modal"><fmt:message key='link.reset'/></button></li>
								</c:if>
							</ul>
						</li>
		
						<li class="nav-item dropdown">
							<a href="#" class="nav-link dropdown-toggle${navloc == 'cli-console' or navloc == 'controls' ? ' active' : ''}" data-bs-toggle="dropdown" role="button" aria-expanded="false">
								<fmt:message key='link.tools'/>
								<b class="caret"></b>
							</a>
							<ul class="dropdown-menu dropdown-menu-md-end">
								<li><a class="dropdown-item${navloc == 'controls' ? ' active' : ''}" href="<setup:url value='/a/controls'/>"><fmt:message key='link.controls'/></a></li>
								<li><a class="dropdown-item${navloc == 'cli-console' ? ' active' : ''}" href="<setup:url value='/a/cli-console'/>"><fmt:message key='link.cli-console'/></a></li>
							</ul>
						</li>
						<li class="nav-item dropdown">
							<a href="#" class="nav-link dropdown-toggle${navloc == 'sectoks' or navloc == 'user' ? ' active' : ''}" data-bs-toggle="dropdown" role="button" aria-expanded="false">
								<span class="active-user-display">
									<fmt:message key='nav.label.principal'>
										<fmt:param><sec:authentication property="principal.username" /></fmt:param>
									</fmt:message>
								</span>
								<b class="caret"></b>
							</a>
							<ul class="dropdown-menu dropdown-menu-md-end">
								<li><a class="dropdown-item" href="<setup:url value='/a/security-tokens'/>"><fmt:message key="link.sectoks"/></a></li>
								<li><a class="dropdown-item" href="<setup:url value='/a/user/change-password'/>"><fmt:message key="link.change-password"/></a></li>
								<li><a class="dropdown-item" href="<setup:url value='/a/user/change-username'/>"><fmt:message key="link.change-username"/></a></li>
								<li><a class="dropdown-item logout" href="#"><fmt:message key='link.logout'/></a></li>
							</ul>
						</li>
					</sec:authorize>
				</ul>
			</div>
		</c:if>
	</div>
</div>
<sec:authorize access="hasRole('ROLE_USER')">
	<form id="logout-form" method="post" action="<setup:url value='/logout'/>">
		<sec:csrfInput/>
	</form>
	<form id="restart-modal" class="modal fade" data-bs-backdrop="static" data-bs-keyboard="false" action="<setup:url value='/a/home/restart'/>" method="post">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<h3 class="modal-title"><fmt:message key='restart.title'/></h3>
					<button type="button" class="btn-close start" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
				</div>
				<div class="modal-body start">
					<p><fmt:message key='restart.intro'/></p>
				</div>
				<div class="modal-body success" style="display: none;">
					<div class="progress" role="progressbar">
						<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
					</div>
					<p class="my-3"><fmt:message key='restart.underway'/></p>
				</div>
				<div class="modal-footer start">
					<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></a>
					<button type="submit" class="btn btn-danger reboot"><fmt:message key="restart.action.reboot"/></button>
					<button type="submit" class="btn btn-primary"><fmt:message key="restart.action.restart"/></button>
				</div>
				<input type="hidden" name="reboot" value="false"/>
			</div>
		</div>
		<sec:csrfInput/>
	</form>
	<form id="reset-modal" class="modal fade" data-bs-backdrop="static" data-bs-keyboard="false" action="<setup:url value='/a/home/reset'/>" method="post">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<h3 class="modal-title"><fmt:message key='reset.title'/></h3>
					<button type="button" class="btn-close start" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
				</div>
				<div class="modal-body start">
					<p><fmt:message key='reset.intro'/></p>
					<div class="form-check">
						<input class="form-check-input" type="checkbox" name="applicationOnly" value="true" id="reset-application-only">
						<label class="form-check-label" for="reset-application-only"><fmt:message key='reset.applicationOnly.label'/></label>
					</div>
				</div>
				<div class="modal-body success" style="display: none;">
					<div class="progress" role="progressbar">
						<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
					</div>
					<p class="my-3"><fmt:message key='reset.underway'/></p>
				</div>
				<div class="modal-footer start">
					<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></a>
					<button type="submit" class="btn btn-danger reboot"><fmt:message key="reset.action.reset"/></button>
				</div>
				<input type="hidden" name="reboot" value="false"/>
			</div>
		</div>
		<sec:csrfInput/>
	</form>
</sec:authorize>
