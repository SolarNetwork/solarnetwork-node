<fmt:message key="packages.upgrade.button" var="msgUpgradeAll"/>
<fmt:message key="package.install.button" var="msgInstall"/>
<fmt:message key="package.upgrade.button" var="msgUpgrade"/>
<fmt:message key="package.remove.button" var="msgRemove"/>
<fmt:message key="package.unremovable.message" var="msgUnremovable"/>

<section class="intro clearfix">
	<button id="packages-refresh" class="btn btn-primary ladda-button expand-right pull-right"
		data-loading-text="<fmt:message key='packages.refreshing.message'/>">
		<fmt:message key="packages.refresh.link"/>
	</button>
	<p><fmt:message key="packages.intro"/></p>
</section>
<div class="init">
	<div class="progress progress-striped active">
		<div class="progress-bar" style="width: 100%;"></div>
    </div>
</div>
<div class="ready hidden">

	<section id="packages-upgradable" class="hidden">
		<h2><fmt:message key="packages.upgradable.title"/></h2>
		<div class="row">
			<div class="col-md-9">
				<p><fmt:message key="packages.upgradable.intro"/></p>
			</div>
			<form class="col-md-3 text-right form-inline">
				<button class="btn btn-info" 
						data-target="#packages-upgrade-modal"
						data-toggle="modal"><fmt:message key="packages.upgrade.button"/></button>
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

	<div class="hide">
		<div id="more-packages"><div class="row">
			<div class="col-md-12"><p class="test-muted"><fmt:message key="packages.more.message"/></p></div>
		</div></div>
	</div>

	<section id="packages-installed" data-msg-remove="${msgRemove}">
		<h2 id="installed-packages"><fmt:message key="packages.installed.title"/></h2>
		<div class="row">
			<div class="col-md-9">
				<p><fmt:message key="packages.installed.intro"/></p>
			</div>
			<form class="col-md-3 text-right form-inline">
				<input type="search" id="installedSearchFilter" 
						class="input-medium search-query"
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
						class="input-medium search-query"
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

<form id="packages-upgrade-modal" class="packages modal hide fade" action="<setup:url value='/a/packages/upgrade'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key="packages.upgrade.title"/></h3>
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
			<button type="button" class="btn pull-left hidden after success restart" title="<fmt:message key='link.restart'/>">
				<fmt:message key="link.restart"/>
			</button>
		</c:if>
		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<button type="submit" class="btn btn-primary ladda-button expand-right before"
				data-loading-text="<fmt:message key='packages.upgrading.message'/>">${msgUpgradeAll}</button>
	</div>
	<sec:csrfInput/>
</form>

<form id="package-install-modal" class="packages modal hide fade" action="<setup:url value='/a/packages/install'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key="package.install.title"/></h3>
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
			<button type="button" class="btn pull-left hidden after success restart" title="<fmt:message key='link.restart'/>">
				<fmt:message key="link.restart"/>
			</button>
		</c:if>
		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<button type="submit" class="btn btn-primary ladda-button expand-right before"
				data-loading-text="<fmt:message key='package.installing.message'/>">${msgInstall}</button>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="name">
</form>

<form id="package-remove-modal" class="packages modal hide fade" action="<setup:url value='/a/packages/remove'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key="package.remove.title"/></h3>
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
			<button type="button" class="btn pull-left hidden after success restart" title="<fmt:message key='link.restart'/>">
				<fmt:message key="link.restart"/>
			</button>
		</c:if>
		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<button type="submit" class="btn btn-danger ladda-button expand-right before"
				data-loading-text="<fmt:message key='package.removing.message'/>">${msgRemove}</button>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="name">
</form>
