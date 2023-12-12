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
<section id="plugin-upgrades" class="hidden" data-msg-upgrade="${msgUpgrade}">
	<h2><fmt:message key="plugins.upgradable.title"/></h2>
	<div class="row">
		<p class="col-md-10"><fmt:message key="plugins.upgradable.intro"/></p>
		<div class="col-md-2 action">
			<setup:url value="/a/plugins/upgradeAll" var="urlPluginUpgradeAll"/>
			<a id="plugins-upgrade-all" class="btn btn-info" href="${urlPluginUpgradeAll}"><fmt:message key="plugins.upgradeAll.button"/></a>
		</div>
	</div>
	<div class="list-content"></div>
</section>
<section id="plugin-installed" class="hidden" data-msg-remove="${msgRemove}" data-msg-unremovable="${msgUnremovable}">
	<h2><fmt:message key="plugins.installed.title"/></h2>
	<p><fmt:message key="plugins.installed.intro"/></p>
	<div class="list-content"></div>
</section>
<section id="plugins" class="hidden" data-msg-install="${msgInstall}" data-msg-upgrade="${msgUpgrade}" data-msg-remove="${msgRemove}">
	<h2><fmt:message key="plugins.available.title"/></h2>
	<p><fmt:message key="plugins.available.intro"/></p>
	<div class="list-content"></div>
</section>

<setup:url value="/a/plugins/install" var="urlPluginInstall"/>
<form id="plugin-preview-install-modal" class="modal fade dynamic" action="${urlPluginInstall}" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title" data-msg-install="${msgInstall}"><fmt:message key="plugin.install.title"/></h3>
				<button type="button" class="btn-close without-restart" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p class="hide-while-restarting"><fmt:message key="plugin.install.intro"/></p>
				<div class="hide-while-restarting" id="plugin-preview-install-list"></div>
				<div class="restart-required hide-while-restarting hide alert">
					<fmt:message key='plugin.install.restartRequired.warning'/>
				</div>
				<div class="restarting hidden alert alert-info">
					<fmt:message key="plugin.install.success"/><span> </span><fmt:message key="restart.underway"/>
				</div>
				<div class="progress hidden" role="progressbar">
					<div class="progress-bar progress-bar-striped progress-bar-animated"></div>
			    </div>
			    <div class="message-container hide-while-restarting"></div>
			</div>
			<div class="modal-footer">
				<input type="hidden" name="uid" value=""/>
				<a href="#" class="btn without-restart" data-bs-dismiss="modal"><fmt:message key="close.label"/></a>
				<fmt:message key="plugin.installing.message" var="msgInstalling"/>
				<fmt:message key="plugin.install.error" var="msgInstallError"/>
				<fmt:message key="plugin.install.success" var="msgInstallSuccess"/>
				<button type="submit" class="btn btn-primary ladda-button expand-right hide-while-restarting"
					data-msg-error="${msgInstallError}"
					data-msg-success="${msgInstallSuccess}"
					data-loading-text="${msgInstalling}">${msgInstall}</button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<setup:url value="/a/plugins/remove" var="urlPluginRemove"/>
<form id="plugin-preview-remove-modal" class="modal fade dynamic" action="${urlPluginRemove}" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title" data-msg-remove="${msgRemove}"><fmt:message key="plugin.remove.title"/></h3>
				<button type="button" class="btn-close without-restart" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p class="hide-while-restarting"><fmt:message key="plugin.remove.intro"/></p>
				<div class="hide-while-restarting" id="plugin-preview-remove-list"></div>
				<div class="restart-required hide-while-restarting hide alert">
					<fmt:message key='plugin.install.restartRequired.warning'/>
				</div>
				<div class="restarting hidden alert alert-info">
					<fmt:message key="plugin.remove.success"/><span> </span><fmt:message key="restart.underway"/>
				</div>
				<div class="progress hidden" role="progressbar">
					<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
			    </div>
			    <div class="message-container hide-while-restarting"></div>
			</div>
			<div class="modal-footer">
				<input type="hidden" name="uid" value=""/>
				<a href="#" class="btn without-restart" data-bs-dismiss="modal"><fmt:message key="close.label"/></a>
				<fmt:message key="plugin.removing.message" var="msgRemoving"/>
				<fmt:message key="plugin.remove.error" var="msgRemoveError"/>
				<fmt:message key="plugin.remove.success" var="msgRemoveSuccess"/>
				<button type="submit" class="btn btn-danger ladda-button expand-right hide-while-restarting"
					data-msg-error="${msgRemoveError}"
					data-msg-success="${msgRemoveSuccess}"
					data-loading-text="${msgRemoving}">${msgRemove}</button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>
