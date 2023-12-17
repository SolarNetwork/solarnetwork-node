<fmt:message key="packages.upgrade.button" var="msgUpgradeAll"/>
<fmt:message key="package.install.button" var="msgInstall"/>
<fmt:message key="package.upgrade.button" var="msgUpgrade"/>
<fmt:message key="package.remove.button" var="msgRemove"/>
<fmt:message key="package.unremovable.message" var="msgUnremovable"/>

<section class="intro">
	<p><fmt:message key="packages.intro"/></p>
	<div class="row justify-content-end">
		<div class="col-auto">
			<button id="packages-refresh" class="btn btn-primary"
				data-loading-text="<fmt:message key='packages.refreshing.message'/>">
				<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
				<span role="status"><fmt:message key="packages.refresh.link"/></span>
			</button>
		</div>
	</div>
</section>
<div class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</div>
<div class="ready hidden">

	<section id="packages-upgradable" class="hidden">
		<h2><fmt:message key="packages.upgradable.title"/></h2>
		<div class="row justify-content-between">
			<div class="col-auto">
				<p><fmt:message key="packages.upgradable.intro"/></p>
			</div>
			<form class="col-auto">
				<button type="button" class="btn btn-info" 
						data-bs-target="#packages-upgrade-modal"
						data-bs-toggle="modal"><fmt:message key="packages.upgrade.button"/></button>
			</form>
		</div>
		<div class="row template hbox" style="align-items: center;">
			<div class="col-md-8">
				<button class="btn btn-link edit-item" data-tprop="name">My package</button>
			</div>
			<div class="col-md-4">
				<span data-tprop="version">1.2.3</span>
			</div>
		</div>
		<div class="list-content">
		</div>
	</section>

	<section id="packages-installed" data-msg-remove="${msgRemove}">
		<h2 id="installed-packages"><fmt:message key="packages.installed.title"/></h2>
		<div class="row justify-content-between">
			<div class="col-md-9">
				<p><fmt:message key="packages.installed.intro"/></p>
			</div>
			<form class="col-md-3 text-right form-inline">
				<input type="search" id="installedSearchFilter" 
						class="form-control search-query"
						placeholder="<fmt:message key='packages.filter.label'/>"
						value="solarnode">
			</form>
		</div>
		
		<div class="row template hbox" style="align-items: center;">
			<div class="col-md-8">
				<button class="btn btn-link edit-item" data-tprop="name">My package</button>
			</div>
			<div class="col-md-4">
				<span data-tprop="version">1.2.3</span>
			</div>
		</div>
		<div class="list-content"></div>
	</section>
	
	<section id="packages">
		<h2 id="available-packages"><fmt:message key="packages.available.title"/></h2>
		<div class="row">
			<div class="col-md-9">
				<p><fmt:message key="packages.available.intro"/></p>
			</div>
			<form class="col-md-3 text-right form-inline">
				<input type="search" id="availableSearchFilter" 
						class="form-control search-query"
						placeholder="<fmt:message key='packages.filter.label'/>"
						value="solarnode">
			</form>
		</div>
		<div class="row template hbox" style="align-items: center;">
			<div class="col-md-8">
				<button class="btn btn-link edit-item" data-tprop="name">My package</button>
			</div>
			<div class="col-md-4">
				<span data-tprop="version">1.2.3</span>
			</div>
		</div>
		<div class="list-content"></div>
	</section>
</div>

<div class="hidden">
	<div id="more-packages"><div class="row">
		<div class="col-md-12"><p class="test-muted"><fmt:message key="packages.more.message"/></p></div>
	</div></div>
</div>


<form id="packages-upgrade-modal" class="packages modal fade" action="<setup:url value='/a/packages/upgrade'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="packages.upgrade.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body before">
				<p><fmt:message key="packages.upgrade.intro"/></p>
			</div>
			<div class="modal-body after hidden">
				<p class="success"><fmt:message key="packages.upgrade.success"/></p>
				<p class="error hidden"><fmt:message key="packages.upgrade.error"/></p>
			</div>
			<div class="modal-footer">
				<c:if test="${not empty systemService}">
					<button type="button" class="btn btn-warning hidden after success restart" title="<fmt:message key='link.restart'/>">
						<fmt:message key="link.restart"/>
					</button>
				</c:if>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></button>
				<button type="submit" class="btn btn-primary before"
						data-loading-text="<fmt:message key='packages.upgrading.message'/>">					
					<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
					<span role="status">${msgUpgradeAll}</span>
				</button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<form id="package-install-modal" class="packages modal fade" action="<setup:url value='/a/packages/install'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="package.install.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body before">
				<p><fmt:message key="package.install.intro"/></p>
			</div>
			<div class="modal-body after hidden">
				<p class="success"><fmt:message key="package.install.success"/></p>
				<p class="error hidden"><fmt:message key="package.install.error"/></p>
			</div>
			<div class="modal-footer">
				<c:if test="${not empty systemService}">
					<button type="button" class="btn btn-warning hidden after success restart" title="<fmt:message key='link.restart'/>">
						<fmt:message key="link.restart"/>
					</button>
				</c:if>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></button>
				<button type="submit" class="btn btn-primary before"
						data-loading-text="<fmt:message key='package.installing.message'/>">
					<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
					<span role="status">${msgInstall}</span>
				</button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="name">
</form>

<form id="package-remove-modal" class="packages modal fade" action="<setup:url value='/a/packages/remove'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="package.remove.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body before">
				<p><fmt:message key="package.remove.intro"/></p>
			</div>
			<div class="modal-body after hidden">
				<p class="success"><fmt:message key="package.remove.success"/></p>
				<p class="error hidden"><fmt:message key="package.remove.error"/></p>
			</div>
			<div class="modal-footer">
				<c:if test="${not empty systemService}">
					<button type="button" class="btn btn-warning hidden after success restart" title="<fmt:message key='link.restart'/>">
						<fmt:message key="link.restart"/>
					</button>
				</c:if>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></button>
				<button type="submit" class="btn btn-danger ladda-button expand-right before"
						data-loading-text="<fmt:message key='package.removing.message'/>">						
					<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
					<span role="status">${msgRemove}</span>
				</button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="name">
</form>
