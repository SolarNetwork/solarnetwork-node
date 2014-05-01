<section class="intro clearfix">
	<p><fmt:message key="plugins.intro"/></p>
	<fmt:message key="plugins.loading.message" var="msgLoading"/>
	<fmt:message key="plugins.refresh.button" var="msgRefresh"/>
	<c:url value="/plugins/refresh" var="urlPluginRefresh"/>
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
	<p><fmt:message key="plugins.upgradable.intro"/></p>
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

<c:url value="/plugins/install" var="urlPluginInstall"/>
<form id="plugin-preview-install-modal" class="modal dynamic hide fade" action="${urlPluginInstall}" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3 data-msg-install="${msgInstall}"><fmt:message key="plugin.install.title"/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key="plugin.install.intro"/></p>
		<div id="plugin-preview-install-list"></div>
		<div class="progress progress-striped active hide">
			<div class="bar"></div>
	    </div>
	    <div class="message-container"></div>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="uid" value=""/>
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<fmt:message key="plugin.installing.message" var="msgInstalling"/>
		<fmt:message key="plugin.install.error" var="msgInstallError"/>
		<fmt:message key="plugin.install.success" var="msgInstallSuccess"/>
		<button type="submit" class="btn btn-primary ladda-button expand-right"
			data-msg-error="${msgInstallError}"
			data-msg-success="${msgInstallSuccess}"
			data-loading-text="${msgInstalling}">${msgInstall}</button>
	</div>
</form>

<c:url value="/plugins/remove" var="urlPluginRemove"/>
<form id="plugin-preview-remove-modal" class="modal dynamic hide fade" action="${urlPluginRemove}" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3 data-msg-remove="${msgRemove}"><fmt:message key="plugin.remove.title"/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key="plugin.remove.intro"/></p>
		<div id="plugin-preview-remove-list"></div>
		<div class="progress progress-striped active hide">
			<div class="bar"></div>
	    </div>
	    <div class="message-container"></div>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="uid" value=""/>
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<fmt:message key="plugin.removing.message" var="msgRemoving"/>
		<fmt:message key="plugin.remove.error" var="msgRemoveError"/>
		<fmt:message key="plugin.remove.success" var="msgRemoveSuccess"/>
		<button type="submit" class="btn btn-danger ladda-button expand-right"
			data-msg-error="${msgRemoveError}"
			data-msg-success="${msgRemoveSuccess}"
			data-loading-text="${msgRemoving}">${msgRemove}</button>
	</div>
</form>
