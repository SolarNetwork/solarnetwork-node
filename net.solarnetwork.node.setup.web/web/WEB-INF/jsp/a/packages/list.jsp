<fmt:message key="package.install.button" var="msgInstall"/>
<fmt:message key="package.upgrade.button" var="msgUpgrade"/>
<fmt:message key="package.remove.button" var="msgRemove"/>
<fmt:message key="package.unremovable.message" var="msgUnremovable"/>

<section class="intro clearfix">
	<p><fmt:message key="packages.intro"/></p>
	<%--
	<fmt:message key="packages.loading.message" var="msgLoading"/>
	<fmt:message key="packages.refresh.button" var="msgRefresh"/>
	<setup:url value="/a/packages/refresh" var="urlPackageRefresh"/>
	<a id="packages-refresh" class="btn btn-primary ladda-button expand-right pull-right" href="${urlPackageRefresh}"
		data-loading-text="${msgLoading}">
		${msgRefresh}
	</a>
	--%>
</section>
<div class="init">
	<div class="progress progress-striped active">
		<div class="bar" style="width: 100%;"></div>
    </div>
</div>
<div class="ready hide">
<%--
<section id="package-upgrades" class="hide" data-msg-upgrade="${msgUpgrade}">
	<h2><fmt:message key="packages.upgradable.title"/></h2>
	<div class="row">
		<p class="span10"><fmt:message key="packages.upgradable.intro"/></p>
		<div class="span2 text-right action">
			<setup:url value="/a/packages/upgradeAll" var="urlPackageUpgradeAll"/>
			<a id="packages-upgrade-all" class="btn btn-info" href="${urlPackageUpgradeAll}"><fmt:message key="packages.upgradeAll.button"/></a>
		</div>
	</div>
	<div class="row template">
		<div class="span6">
			<span data-tprop="name">My package</span>
		</div>
		<div class="span3">
			<span data-tprop="version">1.2.3</span>
		</div>
		<div class="span3 text-right">
		    <button type="button" class="btn btn-small">
		    	<fmt:message key='package.upgrade.button'/>
		    </button>
		</div>
	</div>
	<div class="list-content">
	</div>
</section>
--%>
	<section id="package-installed" data-msg-remove="${msgRemove}">
		<h2><fmt:message key="packages.installed.title"/></h2>
		<div class="row">
			<div class="span9">
				<p><fmt:message key="packages.installed.intro"/></p>
			</div>
			<form class="span3 text-right form-inline">
				<input type="search" id="installedSearchFilter" class="input-medium search-query" placeholder="<fmt:message key='packages.filter.label'/>" value="solarnode">
			</form>
		</div>
		
		<div class="row template">
			<div class="span6">
				<a href="#" class="edit-item" data-tprop="name">My package</a>
			</div>
			<div class="span3">
				<span data-tprop="version">1.2.3</span>
			</div>
		</div>
		<div class="list-content"></div>
	</section>
	
	<section id="packages" data-msg-install="${msgInstall}" data-msg-upgrade="${msgUpgrade}" data-msg-remove="${msgRemove}">
		<h2><fmt:message key="packages.available.title"/></h2>
		<div class="row">
			<div class="span9">
				<p><fmt:message key="packages.available.intro"/></p>
			</div>
			<form class="span3 text-right form-inline">
				<input type="search" id="availableSearchFilter" class="input-medium search-query" placeholder="<fmt:message key='packages.filter.label'/>" value="solarnode">
			</form>
		</div>
		<div class="row template">
			<div class="span6">
				<a href="#" class="edit-item" data-tprop="name">My package</a>
			</div>
			<div class="span3">
				<span data-tprop="version">1.2.3</span>
			</div>
		</div>
		<div class="list-content" style="max-height: 50rem; overflow-y: auto;"></div>
	</section>

</div>

<setup:url value="/a/packages/install" var="urlPackageInstall"/>
<form id="package-info" class="modal dynamic hide fade" action="${urlPackageInstall}" method="post">
	<div class="modal-header">
		<button type="button" class="close without-restart" data-dismiss="modal">&times;</button>
		<h3 data-msg-install="${msgInstall}"><fmt:message key="package.install.title"/></h3>
	</div>
	<div class="modal-body">
		<p class="hide-while-restarting"><fmt:message key="package.install.intro"/></p>
		<div class="hide-while-restarting" id="package-preview-install-list"></div>
		<div class="restart-required hide-while-restarting hide alert">
			<fmt:message key='package.install.restartRequired.warning'/>
		</div>
		<div class="restarting hide alert alert-info">
			<fmt:message key="package.install.success"/><span> </span><fmt:message key="restart.underway"/>
		</div>
		<div class="progress progress-striped active hide">
			<div class="bar"></div>
	    </div>
	    <div class="message-container hide-while-restarting"></div>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="uid" value=""/>
		<a href="#" class="btn without-restart" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<fmt:message key="package.installing.message" var="msgInstalling"/>
		<fmt:message key="package.install.error" var="msgInstallError"/>
		<fmt:message key="package.install.success" var="msgInstallSuccess"/>
		<button type="submit" class="btn btn-primary ladda-button expand-right hide-while-restarting"
			data-msg-error="${msgInstallError}"
			data-msg-success="${msgInstallSuccess}"
			data-loading-text="${msgInstalling}">${msgInstall}</button>
	</div>
	<sec:csrfInput/>
</form>
