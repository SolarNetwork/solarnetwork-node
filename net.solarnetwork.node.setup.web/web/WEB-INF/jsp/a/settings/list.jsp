<section class="intro">
	<p>
		<fmt:message key="settings.intro"/>
	</p>
</section>

<c:if test="${fn:length(factories) > 0}">
	<section id="factories">
		<h2>
			<a id="components-section" href="#components-section"
				class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
			<fmt:message key="settings.factories.title"/>
		</h2>
		<p><fmt:message key="settings.factories.intro"/></p>
		<div class="row gap-3 setting-components">
		<c:forEach items="${factories}" var="factory" varStatus="factoryStatus">
			<div class="row justify-content-between align-items-center">
				<!--  ${factory.factoryUid} -->
				<div class="col">
					<div class="row">
						<div class="col-2 col-sm-1">
							<span class="badge rounded-pill text-bg-primary${fn:length(factory.settingSpecifierProviderInstanceIds) lt 1 ? ' invisible' : ''}"
								title="<fmt:message key='settings.factories.instanceCount.caption'/>">${fn:length(factory.settingSpecifierProviderInstanceIds)}</span>
							<c:if test="${fn:length(factory.settingSpecifierProviderInstanceIds) > 0}">
							</c:if>
						</div>
						<div class="col"><strong><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></strong></div>
					</div>
				</div>
				<div class="col-auto">
					<a class="btn btn-secondary" href="<setup:url value='/a/settings/manage?uid=${factory.factoryUid}'/>">
						<i class="far fa-pen-to-square"></i> 
						<fmt:message key="settings.factory.manage.label"/>
					</a>
				</div>
			</div>
		</c:forEach>
		</div>
	</section>
</c:if>

<c:if test="${fn:length(providers) > 0}">
	<section id="settings">
		<h2>
			<a id="settings-section" href="#settings-section"
				class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
			<fmt:message key="settings.providers.title"/>
		</h2>
		<p><fmt:message key="settings.providers.intro"/></p>	

		<form class="form-horizontal" action="<setup:url value='/a/settings/save'/>" method="post">
			<c:forEach items="${providers}" var="provider" varStatus="providerStatus">
				<!--  ${provider.settingUid} -->
				<c:set var="provider" value="${provider}" scope="request"/>
				<fieldset>
					<legend>
						<a id="${provider.settingUid}" 
							class="anchor" 
							href="#${provider.settingUid}"
							aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>
						<setup:message key="title" messageSource="${provider.messageSource}" text="${provider.displayName}"/>
					</legend>
					<c:set var="providerDescription">
						<setup:message key="desc" messageSource="${provider.messageSource}" text=""/>
					</c:set>
					<c:if test="${fn:length(providerDescription) > 0}">
						<p>${providerDescription}</p>
					</c:if>
					<c:catch var="providerException">
						<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
							<c:set var="settingId" value="s${providerStatus.index}i${settingStatus.index}" scope="request"/>
							<c:set var="setting" value="${setting}" scope="request"/>
							<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
						</c:forEach>
					</c:catch>
					<c:if test="${not empty providerException}">
						<div class="alert alert-warning">
							<fmt:message key="settings.error.provider.exception">
								<fmt:param value="${providerException.cause.message}"/>
							</fmt:message>
						</div>
					</c:if>
					<c:if test="${not empty settingResources[provider.settingUid]}">
					<div class="form-group">
						<label class="control-label" for="settings-resource-ident">
							<fmt:message key="settings.io.exportResource.label"/>
						</label>
						<div class="controls">
							<select class="settings-resource-ident">
								<c:forEach items="${settingResources[provider.settingUid]}" var="resource">
									<option data-handler="${resource.handlerKey}" data-key="${resource.key}">${resource.name}</option>
								</c:forEach>
							</select> 
  							<a class="btn btn-primary settings-resource-export" href="<setup:url value='/a/settings/exportResources'/>"><fmt:message key="settings.io.export.button"/></a>
						</div>
					</div>
					</c:if>
				</fieldset>				
			</c:forEach>
			<div class="form-actions">
				<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
			</div>
			<sec:csrfInput/>
		</form>
	</section>
	<script>
	$(function() {
		$('#submit').on('click', function() {
			SolarNode.Settings.saveUpdates($(this.form).attr('action'), {
				success: '<fmt:message key="settings.save.success.msg"/>',
				error: '<fmt:message key="settings.save.error.msg"/>',
				title: '<fmt:message key="settings.save.result.title"/>',
				button: '<fmt:message key="ok.label"/>'
			});
		});
		SolarNode.Settings.reset();
	});
	</script>
</c:if>

<c:if test="${not empty backupManager}">
<section>
	<h2>
		<a id="backup-section" href="#backup-section"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="backup.title"/>
	</h2>
	<p><fmt:message key="backup.intro"/></p>
	<div class="form-horizontal">
		<fieldset>
			<c:set var="provider" value="${backupManager}" scope="request"/>
			<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
				<c:set var="settingId" value="bm${providerStatus.index}i${settingStatus.index}" scope="request"/>
				<c:set var="setting" value="${setting}" scope="request"/>
				<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
			</c:forEach>
			<c:if test="${not empty backupService}">
				<c:set var="provider" value="${backupService.settingSpecifierProvider}" scope="request"/>
				<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
					<c:set var="settingId" value="bs${providerStatus.index}i${settingStatus.index}" scope="request"/>
					<c:set var="setting" value="${setting}" scope="request"/>
					<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
				</c:forEach>
			</c:if>
		</fieldset>

		<fieldset style="margin-top: 24px;">
			<div class="form-group">
				<label class="control-label" for="backup-backups">
					<fmt:message key="backup.now.label"/>
				</label>
				<form class="controls form-inline" id="create-backup-form" action="<setup:url value='/a/backups/create'/>" method="post">
	 				<button class="btn btn-primary ladda-button expand-right" type="submit" id="backup-now-btn"
	 					data-loading-text=" "><fmt:message key="backup.now.button"/></button>
 					<button type="button" class="help-popover help-icon" tabindex="-1"
							data-content="<fmt:message key='backup.now.caption'/>"
							data-html="true">
						<i class="far fa-question-circle" aria-hidden="true"></i>
					</button>
					<sec:csrfInput/>
				</form>
			</div>

			<div class="form-group">
				<label class="control-label" for="backup-backups">
					<fmt:message key="backup.backups.label"/>
				</label>
				<form class="controls form-inline" id="backup-list-form" action="<setup:url value='/a/settings/exportBackup'/>">
					<select name="backup" class="col-md-3" id="backup-backups">
						<c:forEach items="${backups}" var="backup" varStatus="backupStatus">
							<option value="${backup.key}">
								<fmt:message key="backup.backups.backup.label">
									<fmt:param value="${backup.nodeId}"/>
									<fmt:param>
										<fmt:formatDate value="${backup.date}" pattern="dd MMM yyyy HH:mm"/>
									</fmt:param>
								</fmt:message>
								<c:if test="${not empty backup.qualifier}">
									&mdash; ${backup.qualifier}
								</c:if>
							</option>
						</c:forEach>
					</select>
					<button type="submit" class="btn btn-primary">
						<fmt:message key="backup.download.button"/>
					</button>
					<button type="button" class="btn btn-warning" id="backup-restore-button">
						<fmt:message key="backup.restore.button"/>
					</button>
					<button type="button" class="help-popover help-icon" tabindex="-1"
							data-content="<fmt:message key='backup.backups.info'/>"
							data-html="true">
						<i class="far fa-question-circle" aria-hidden="true"></i>
					</button>
					<sec:csrfInput/>
				</form>
			</div>

			<div class="form-group">
				<label class="control-label" for="backup-import-field">
					<fmt:message key="backup.import.label"/>
				</label>
				<form class="controls form-inline" action="<setup:url value='/a/settings/importBackup'/>" method="post" enctype="multipart/form-data">
  					<input class="col-md-3" id="backup-import-field" type="file" name="file"/>
  					<button class="btn btn-primary" type="submit"><fmt:message key="backup.import.button"/></button>
					<button type="button" class="help-popover help-icon" tabindex="-1"
							data-content="<fmt:message key='backup.import.info'/>"
							data-html="true">
						<i class="far fa-question-circle" aria-hidden="true"></i>
					</button>
					<sec:csrfInput/>
				</form>
			</div>
		</fieldset>
	</div>
</section>
</c:if>

<section>
	<h2>
		<a id="settings-backup-section" href="#settings-backup-section"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="settings.io.title"/>
	</h2>
	<p><fmt:message key="settings.io.intro"/></p>
	<div class="form-horizontal">
		<fieldset>
			<div class="form-group">
				<label class="control-label" for="export.btn">
					<fmt:message key="settings.io.export.label"/>
				</label>
				<div class="controls">
					<a class="btn btn-primary" id="export.btn" href="<setup:url value='/a/settings/export'/>">
						<fmt:message key="settings.io.export.button"/>
					</a>
				</div>
			</div>
			<div class="form-group">
				<label class="control-label" for="import.field">
					<fmt:message key="settings.io.import.label"/>
				</label>
				<form class="controls form-inline" action="<setup:url value='/a/settings/import'/>" method="post" enctype="multipart/form-data">
  					<input class="col-md-3" id="import.field" type="file" name="file"/>
  					<button class="btn btn-primary" type="submit"><fmt:message key="settings.io.import.button"/></button>
					<sec:csrfInput/>
				</form>
			</div>
			<c:if test="${not empty settingResources}">
			<div class="form-group">
				<label class="control-label" for="settings-resource-ident">
					<fmt:message key="settings.io.exportResource.label"/>
				</label>
				<form class="controls">
					<select class="settings-resource-ident">
						<c:forEach items="${settingResources}" var="e">
							<c:forEach items="${e.value}" var="resource">
								<option data-handler="${resource.handlerKey}" data-key="${resource.key}">${resource.name}</option>
							</c:forEach>
						</c:forEach>
					</select> 
  					<a class="btn btn-primary settings-resource-export" href="<setup:url value='/a/settings/exportResources'/>"><fmt:message key="settings.io.export.button"/></a>
				</form>
			</div>
			</c:if>
			<c:if test="${fn:length(settingsBackups) > 0}">
				<div class="form-group">
					<label class="control-label" for="auto-backups">
						<fmt:message key="settings.autobackup.label"/>
					</label>
					<div class="controls">
						<ul id="auto-backups">
							<c:forEach items="${settingsBackups}" var="backup" varStatus="backupStatus">
								<li>
				  					<a class="btn btn-sm" id="export.btn" href="<setup:url value='/a/settings/export'/>?backup=${backup.backupKey}">
										<fmt:message key="settings.autobackup.download.button">
											<fmt:param value="${backup.standardDateString}"/>
										</fmt:message>
									</a>
									<c:if test="${backupStatus.first}">
										<button type="button" class="help-popover help-icon" tabindex="-1"
												data-content="<fmt:message key='settings.autobackup.info'/>"
												data-html="true">
											<i class="far fa-question-circle" aria-hidden="true"></i>
										</button>
									</c:if>
								</li>
							</c:forEach>
						</ul>
					</div>
				</div>
			</c:if>
		</fieldset>
	</div>
</section>

<form id="backup-restore-modal" class="modal fade dynamic" data-bs-backdrop="static" data-bs-keyboard="false" action="<setup:url value='/a/backups/restore'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='backup.restore.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='backup.restore.intro'/></p>
				<div id="backup-restore-list-container" class="menu-list noselect" 
					data-msg-items="<fmt:message key='items'/>" data-msg-item="<fmt:message key='item'/>"></div>
				<div class="progress hidden" role="progressbar">
					<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
		    	</div>		
			</div>
			<div class="modal-footer">
				<sec:csrfInput/>
				<input type="hidden" name="key" value=""/>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-danger ladda-button expand-right"><fmt:message key="backup.restore.button"/></button>
			</div>
		</div>
	</div>
</form>
