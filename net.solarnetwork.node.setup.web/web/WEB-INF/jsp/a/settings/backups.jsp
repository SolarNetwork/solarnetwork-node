<section>
	<h2>
		<a id="settings-backup-section" href="#settings-backup-section"
			class="anchor" aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
		<fmt:message key="settings.io.title"/>
	</h2>
	<p>
		<fmt:message key="settings.io.intro">
			<fmt:param><setup:url value="#backup-section"/></fmt:param>
		</fmt:message>
	</p>

	<div class="row">
		<label class="col-sm-4 col-md-3 col-form-label" for="export.btn">
			<fmt:message key="settings.io.export.label"/>
		</label>
		<div class="col-sm-7 col-md-8">
			<a class="btn btn-primary" id="export.btn" href="<setup:url value='/a/settings/export'/>">
				<fmt:message key="settings.io.export.button"/>
			</a>
		</div>
	</div>
	<form class="row mt-3" action="<setup:url value='/a/settings/import'/>" method="post" enctype="multipart/form-data">
		<label class="col-sm-4 col-md-3 col-form-label" for="import.field">
			<fmt:message key="settings.io.import.label"/>
		</label>
		<div class="col-sm-7 col-md-8">
			<div class="input-group mb-3">
				<input class="form-control" id="import.field" type="file" name="file" aria-describedby="settings-il-import-button" required>
				<button class="btn btn-primary" id="settings-il-import-button" type="submit"><fmt:message key="settings.io.import.button"/></button>
			</div>
		</div>
		<sec:csrfInput/>
	</form>
	<c:if test="${not empty settingResourceList}">
		<form class="row mt-3">
			<label class="col-sm-4 col-md-3 col-form-label" for="settings-resource-ident">
				<fmt:message key="settings.io.exportResource.label"/>
			</label>
			<div class="col-sm-7 col-md-8">
				<div class="input-group">
					<select class="form-select settings-resource-ident" id="settings-io-export-ident">
						<c:forEach items="${settingResourceList}" var="resource">
							<option data-handler="${resource.handlerKey}" data-key="${resource.key}">${resource.name}</option>
						</c:forEach>
					</select>
					<button type="button" class="btn btn-primary settings-resource-export"
						data-action="<setup:url value='/a/settings/exportResources'/>"
						data-target="#settings-io-export-ident"
						><fmt:message key="settings.io.export.button"/></button>
				</div>
			</div>
		</form>
	</c:if>
	<c:if test="${fn:length(settingsBackups) > 0}">
		<div class="row mt-3">
			<label class="col-sm-4 col-md-3 col-form-label" for="auto-backups">
				<fmt:message key="settings.autobackup.label"/>
			</label>
			<div class="col-sm-7 col-md-8 pt-1">
				<c:forEach items="${settingsBackups}" var="backup" varStatus="backupStatus">
					<p>
	  					<a class="btn btn-sm btn-link" href="<setup:url value='/a/settings/export'/>?backup=${backup.backupKey}">
							<fmt:message key="settings.autobackup.download.button">
								<fmt:param value="${backup.standardDateString}"/>
							</fmt:message>
						</a>
					</p>
				</c:forEach>
			</div>
			<div class="col-sm-1 mt-1">
				<button type="button" class="help-popover help-icon" title="<fmt:message key='help.label'/>" tabindex="-1"
						data-bs-content="<fmt:message key='settings.autobackup.info'/>"
						data-bs-html="true">
					<i class="bi bi-question-circle" aria-hidden="true"></i>
				</button>
			</div>
		</div>
	</c:if>
</section>

<section class="intro">
	<h2>
		<a id="backup-section" href="#backup-section"
			class="anchor" aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
		<fmt:message key="backup.title"/>
	</h2>
	<c:choose>
		<c:when test="${empty backupManager}">
			<p><fmt:message key="backup.unavailable"/></p>
		</c:when>
		<c:otherwise>
			<p>
				<fmt:message key="backup.intro">
					<fmt:param><setup:url value="#settings-backup-section"/></fmt:param>
				</fmt:message>
			</p>


			<form id="backups-choose-backup-form" action="<setup:url value='/a/settings/save'/>" method="post">
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
				
				<div class="form-actions d-grid my-5">
					<button type="button" class="btn btn-primary settings-save" id="backups-apply-settings" disabled><fmt:message key='backup.settings.save'/></button>
				</div>
			</form>		
			<script>
			$(function() {
				$('#backups-apply-settings').on('click', function() {
					SolarNode.Settings.saveUpdates($(this.form).attr('action'), {
						success: '<fmt:message key="settings.save.success.msg"/>',
						error: '<fmt:message key="settings.save.error.msg"/>',
						title: '<fmt:message key="settings.save.result.title"/>',
						button: '<fmt:message key="ok.label"/>'
					});
				});
			});
			</script>
		
			<form class="row mt-5" id="create-backup-form" action="<setup:url value='/a/backups/create'/>" method="post">
				<label class="col-sm-4 col-md-3 col-form-label" for="backup-backups">
					<fmt:message key="backup.now.label"/>
				</label>
				<div class="col-sm-7 col-md-8">
					<button class="btn btn-primary" type="submit" id="backup-now-btn" data-loading-text=" ">
						<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
						<span role="status"><fmt:message key="backup.now.button"/></span>
					</button>
				</div>
				<div class="col-sm-1 mt-1">
					<button type="button" class="help-popover help-icon" title="<fmt:message key='help.label'/>" tabindex="-1"
						data-bs-content="<fmt:message key='backup.now.caption'/>"
						data-bs-html="true">
						<i class="bi bi-question-circle" aria-hidden="true"></i>
					</button>
				</div>
				<sec:csrfInput/>
			</form>
		
			<form class="row mt-3" id="backup-list-form" action="<setup:url value='/a/settings/exportBackup'/>">
				<label class="col-sm-4 col-md-3 col-form-label" for="backup-backups">
					<fmt:message key="backup.backups.label"/>
				</label>
				<div class="col-sm-7 col-md-8">
					<div class="row g-3">
						<div class="col-sm-6">
							<select name="backup" class="form-select" id="backup-backups">
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
						</div>
						<div class="col-auto">
							<button type="submit" class="btn btn-primary">
								<fmt:message key="backup.download.button"/>
							</button>
						</div>
						<div class="col-auto">
							<button type="button" class="btn btn-warning" id="backup-restore-button">
								<fmt:message key="backup.restore.button"/>
							</button>
						</div>
					</div>
				</div>
				<div class="col-sm-1 mt-1">
					<button type="button" class="help-popover help-icon" title="<fmt:message key='help.label'/>" tabindex="-1"
							data-bs-content="<fmt:message key='backup.backups.info'/>"
							data-bs-html="true">
						<i class="bi bi-question-circle" aria-hidden="true"></i>
					</button>
				</div>
				<sec:csrfInput/>
			</form>
		
			<form class="row mt-3" action="<setup:url value='/a/settings/importBackup'/>" method="post" enctype="multipart/form-data">
				<label class="col-sm-4 col-md-3 col-form-label" for="backup-import-field">
					<fmt:message key="backup.import.label"/>
				</label>
				<div class="col-sm-7 col-md-8">
					<div class="input-group">
						<input class="form-control" id="backup-import-field" type="file" name="file" aria-describedby="backup-import-button" required>
						<button class="btn btn-primary" id="backup-import-button" type="submit"><fmt:message key="backup.import.button"/></button>
					</div>
				</div>
				<div class="col-sm-1 mt-1">
					<button type="button" class="help-popover help-icon" title="<fmt:message key='help.label'/>" tabindex="-1"
							data-bs-content="<fmt:message key='backup.import.info'/>"
							data-bs-html="true">
						<i class="bi bi-question-circle" aria-hidden="true"></i>
					</button>
				</div>
				<sec:csrfInput/>
			</form>
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
		</c:otherwise>
	</c:choose>
</section>