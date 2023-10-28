<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<sec:authorize access="isAuthenticated()" var="isLoggedIn"/>
<div class="navbar">
	<div class="navbar-inner">
		<a class="brand" href="<setup:url value='${isLoggedIn ? "/a/home" : "/"}'/>">
			<img src="<setup:url value='/img/logo-node.svg'/>" alt="<fmt:message key='app.name'/>" width="143" height="28"/>	
		</a>
		<ul class="nav pull-right">
			<li ${navloc == 'home' and isLoggedIn ? 'class="active"' : ''}>
				<a href="<setup:url value='/a/home'/>">
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
				<li class="dropdown${navloc == 'settings'
						or navloc == 'settings-component'
						or navloc == 'filters'
						or navloc == 'filters-component'
						or navloc == 'opmodes' ? ' active' : ''}">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
						<fmt:message key='link.settings'/>
						<b class="caret"></b>
					</a>
					<ul class="dropdown-menu">
						<li ${navloc == 'settings' ? 'class="active"' : ''}><a href="<setup:url value='/a/settings'/>"><fmt:message key='link.settings'/></a></li>
						<li ${navloc == 'filters' ? 'class="active"' : ''}><a href="<setup:url value='/a/settings/filters'/>"><fmt:message key='link.filters'/></a></li>
						<li ${navloc == 'logging' ? 'class="active"' : ''}><a href="<setup:url value='/a/logging'/>"><fmt:message key='link.logging'/></a></li>
						<li ${navloc == 'opmodes' ? 'class="active"' : ''}><a href="<setup:url value='/a/opmodes'/>"><fmt:message key='link.opmodes'/></a></li>
					</ul>
				</li>
				
				<li class="dropdown${navloc == 'cert' or navloc == 'plugins' or navloc == 'packages' ? ' active' : ''}">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
						<fmt:message key='link.system'/>
						<b class="caret"></b>
					</a>
					<ul class="dropdown-menu">
						<li ${navloc == 'cert' ? 'class="active"' : ''}><a href="<setup:url value='/a/certs'/>"><fmt:message key='link.cert'/></a></li>
						<c:if test="${not empty platformPackageService}">
							<li ${navloc == 'packages' ? 'class="active"' : ''}><a id="link-packages" href="<setup:url value='/a/packages'/>"><fmt:message key='link.packages'/></a></li>
						</c:if>
						<li ${navloc == 'plugins' ? 'class="active"' : ''}><a id="link-plugins" href="<setup:url value='/a/plugins'/>"><fmt:message key='link.plugins'/></a></li>
						<c:if test="${not empty systemService}">
							<li role="separator" class="divider"></li>
							<li><a class="restart" href="#"><fmt:message key='link.restart'/></a></li>
							<li role="separator" class="divider"></li>
							<li><a class="reset" href="#"><fmt:message key='link.reset'/></a></li>
						</c:if>
					</ul>
				</li>
				
				<li class="dropdown${navloc == 'cli-console' or navloc == 'controls' ? ' active' : ''}">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
						<fmt:message key='link.tools'/>
						<b class="caret"></b>
					</a>
					<ul class="dropdown-menu">
						<li ${navloc == 'controls' ? 'class="active"' : ''}><a href="<setup:url value='/a/controls'/>"><fmt:message key='link.controls'/></a></li>
						<li ${navloc == 'cli-console' ? 'class="active"' : ''}><a href="<setup:url value='/a/cli-console'/>"><fmt:message key='link.cli-console'/></a></li>
					</ul>
				</li>
				<li class="dropdown${navloc == 'sectoks' or navloc == 'user' ? ' active' : ''}">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
						<span class="active-user-display">
							<fmt:message key='nav.label.principal'>
								<fmt:param><sec:authentication property="principal.username" /></fmt:param>
							</fmt:message>
						</span>
						<b class="caret"></b>
					</a>
					<ul class="dropdown-menu">
						<li><a href="<setup:url value='/a/security-tokens'/>"><fmt:message key="link.sectoks"/></a></li>
						<li><a href="<setup:url value='/a/user/change-password'/>"><fmt:message key="link.change-password"/></a></li>
						<li><a href="<setup:url value='/a/user/change-username'/>"><fmt:message key="link.change-username"/></a></li>
						<li><a class="logout" href="#"><fmt:message key='link.logout'/></a></li>
					</ul>
				</li>
			</sec:authorize>
		</ul>
		<sec:authorize access="hasRole('ROLE_USER')">
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
			<form id="reset-modal" class="modal hide fade" action="<setup:url value='/a/home/reset'/>" method="post">
				<div class="modal-header">
					<button type="button" class="close start" data-dismiss="modal">&times;</button>
					<h3><fmt:message key='reset.title'/></h3>
				</div>
				<div class="modal-body start">
					<p><fmt:message key='reset.intro'/></p>
					<label class="checkbox">
						<input type="checkbox" name="applicationOnly" value="true"/>
						<fmt:message key='reset.applicationOnly.label'/>
					</label>
				</div>
				<div class="modal-body success" style="display: none;">
					<div class="progress progress-info progress-striped active">
						<div class="bar" style="width:100%"></div>
					</div>
					<p><fmt:message key='reset.underway'/></p>
				</div>
				<div class="modal-footer start">
					<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
					<button type="submit" class="btn btn-danger reboot"><fmt:message key="reset.action.reset"/></button>
				</div>
				<input type="hidden" name="reboot" value="false"/>
				<sec:csrfInput/>
			</form>
		</sec:authorize>
	</div>
</div>
