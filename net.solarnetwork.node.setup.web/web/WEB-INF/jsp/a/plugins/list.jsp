<section class="intro clearfix">
	<p><fmt:message key="plugins.intro"/></p>
	<fmt:message key="plugins.loading.message" var="msgLoading"/>
	<fmt:message key="plugins.refresh.button" var="msgRefresh"/>
	<setup:url value="/a/plugins/refresh" var="urlPluginRefresh"/>
	<a id="plugins-refresh" class="btn btn-primary ladda-button expand-right pull-right" href="${urlPluginRefresh}"
		data-loading-text="${msgLoading}">
		${msgRefresh}
	</a>
</section>
<fmt:message key="plugin.install.button" var="msgInstall"/>
<fmt:message key="plugin.upgrade.button" var="msgUpgrade"/>
<fmt:message key="plugin.remove.button" var="msgRemove"/>
<fmt:message key="plugin.unremovable.message" var="msgUnremovable"/>
<section id="plugin-upgrades" class="hide" data-msg-upgrade="${msgUpgrade}">
	<h2><fmt:message key="plugins.upgradable.title"/></h2>
	<div class="row-fluid">
		<p class="span10"><fmt:message key="plugins.upgradable.intro"/></p>
		<div class="span2 action">
			<setup:url value="/a/plugins/upgradeAll" var="urlPluginUpgradeAll"/>
			<a id="plugins-upgrade-all" class="btn btn-info" href="${urlPluginUpgradeAll}"><fmt:message key="plugins.upgradeAll.button"/></a>
		</div>
	</div>
	<div class="list-content"></div>
</section>
<section id="plugin-installed" class="hide" data-msg-remove="${msgRemove}" data-msg-unremovable="${msgUnremovable}">
	<h2><fmt:message key="plugins.installed.title"/></h2>
	<p><fmt:message key="plugins.installed.intro"/></p>
	<div class="list-content"></div>
</section>
<section id="plugins" class="hide" data-msg-install="${msgInstall}" data-msg-upgrade="${msgUpgrade}" data-msg-remove="${msgRemove}">
	<h2><fmt:message key="plugins.available.title"/></h2>
	<p><fmt:message key="plugins.available.intro"/></p>
	<div class="list-content"></div>
</section>

<setup:url value="/a/plugins/install" var="urlPluginInstall"/>
<form id="plugin-preview-install-modal" class="modal dynamic hide fade" action="${urlPluginInstall}" method="post">
	<div class="modal-header">
		<button type="button" class="close without-restart" data-dismiss="modal">&times;</button>
		<h3 data-msg-install="${msgInstall}"><fmt:message key="plugin.install.title"/></h3>
	</div>
	<div class="modal-body">
		<p class="hide-while-restarting"><fmt:message key="plugin.install.intro"/></p>
		<div class="hide-while-restarting" id="plugin-preview-install-list"></div>
		<div class="restart-required hide-while-restarting hide alert">
			<fmt:message key='plugin.install.restartRequired.warning'/>
		</div>
		<div class="restarting hide alert alert-info">
			<fmt:message key="plugin.install.success"/><span> </span><fmt:message key="restart.underway"/>
		</div>
		<div class="progress progress-striped active hide">
			<div class="bar"></div>
	    </div>
	    <div class="message-container hide-while-restarting"></div>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="uid" value=""/>
		<a href="#" class="btn without-restart" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<fmt:message key="plugin.installing.message" var="msgInstalling"/>
		<fmt:message key="plugin.install.error" var="msgInstallError"/>
		<fmt:message key="plugin.install.success" var="msgInstallSuccess"/>
		<button type="submit" class="btn btn-primary ladda-button expand-right hide-while-restarting"
			data-msg-error="${msgInstallError}"
			data-msg-success="${msgInstallSuccess}"
			data-loading-text="${msgInstalling}">${msgInstall}</button>
	</div>
	<sec:csrfInput/>
</form>

<setup:url value="/a/plugins/remove" var="urlPluginRemove"/>
<form id="plugin-preview-remove-modal" class="modal dynamic hide fade" action="${urlPluginRemove}" method="post">
	<div class="modal-header">
		<button type="button" class="close without-restart" data-dismiss="modal">&times;</button>
		<h3 data-msg-remove="${msgRemove}"><fmt:message key="plugin.remove.title"/></h3>
	</div>
	<div class="modal-body">
		<p class="hide-while-restarting"><fmt:message key="plugin.remove.intro"/></p>
		<div class="hide-while-restarting" id="plugin-preview-remove-list"></div>
		<div class="restart-required hide-while-restarting hide alert">
			<fmt:message key='plugin.install.restartRequired.warning'/>
		</div>
		<div class="restarting hide alert alert-info">
			<fmt:message key="plugin.remove.success"/><span> </span><fmt:message key="restart.underway"/>
		</div>
		<div class="progress progress-striped active hide">
			<div class="bar"></div>
	    </div>
	    <div class="message-container hide-while-restarting"></div>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="uid" value=""/>
		<a href="#" class="btn without-restart" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<fmt:message key="plugin.removing.message" var="msgRemoving"/>
		<fmt:message key="plugin.remove.error" var="msgRemoveError"/>
		<fmt:message key="plugin.remove.success" var="msgRemoveSuccess"/>
		<button type="submit" class="btn btn-danger ladda-button expand-right hide-while-restarting"
			data-msg-error="${msgRemoveError}"
			data-msg-success="${msgRemoveSuccess}"
			data-loading-text="${msgRemoving}">${msgRemove}</button>
	</div>
	<sec:csrfInput/>
</form>
