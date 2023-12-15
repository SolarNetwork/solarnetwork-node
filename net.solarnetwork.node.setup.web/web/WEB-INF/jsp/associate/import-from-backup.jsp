<%--
	Expects the following request-or-higher properties:
	
	settingsService 	- the SettingService
	backupManager 		- the BackupManager
--%>
<p><fmt:message key="node.setup.restore.intro"/></p>

<section>
	<setup:url value="/associate/importBackup" var="action"/>
	<form:form action="${action}" method="post" enctype="multipart/form-data" id="associate-import-backup-form">
		<fieldset>
			<div class="row">
				<label class="col-sm-4 col-md-3 col-form-label" for="restore-file"><fmt:message key="node.setup.restore.file"/></label>
				<div class="col-sm-7 col-md-8">
					<div class="input-group">
						<input class="form-control" type="file" name="file" id="restore-file">
						<button type="submit" class="btn btn-primary">
							<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
							<span role="status"><fmt:message key='node.setup.restore.upload'/></span>
						</button>
					</div>
				</div>
			</div>
		</fieldset>
		<sec:csrfInput/>
	</form:form>
</section>

<c:if test="${not empty backupManager}">
<section class="my-3">
	<p><fmt:message key="node.setup.restore.backup.intro"/></p>
	<setup:url value="/associate/chooseBackup" var="action"/>
	<form:form action="${action}" method="post" id="associate-choose-backup-form">
		<fieldset>
			<c:set var="provider" value="${backupManager}" scope="request"/>
			<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
				<c:set var="settingId" value="bm${providerStatus.index}i${settingStatus.index}" scope="request"/>
				<c:set var="setting" value="${setting}" scope="request"/>
				<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
			</c:forEach>
			<c:if test="${not empty backupService}">
				<c:set var="provider" value="${backupService.settingSpecifierProviderForRestore}" scope="request"/>
				<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
					<c:set var="settingId" value="bs${providerStatus.index}i${settingStatus.index}" scope="request"/>
					<c:set var="setting" value="${setting}" scope="request"/>
					<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
				</c:forEach>
			</c:if>
		</fieldset>
	
		<fieldset style="mt-3">
			<div class="row">
				<label class="col-sm-4 col-md-3 col-form-label" for="backup-backups"><fmt:message key="backup.backups.label"/></label>
				<div class="col-sm-7 col-md-8">
					<select name="backup" class="form-select" id="backup-backups">
						<c:forEach items="${backups}" var="backup" varStatus="backupStatus">
							<option value="${backup.key}">
								<fmt:message key="backup.backups.backup.label">
									<fmt:param value="${backup.nodeId}"/>
									<fmt:param>
										<fmt:formatDate value="${backup.date}" pattern="dd MMM yyyy HH:mm"/>
									</fmt:param>
								</fmt:message>
							</option>
						</c:forEach>
					</select>
				</div>
			</div>
		</fieldset>
	
		<div class="row mt-3">
			<div class="offset-sm-4 offset-md-3">
				<button type="button" class="btn btn-info" id="associate-choose-backup-apply-settings"><fmt:message key='node.setup.restore.backup.settings.save'/></button>
				<button type="submit" class="btn btn-primary ladda-button expand-right"><fmt:message key="backup.restore.button"/></button>
			</div>
		</div>
		<sec:csrfInput/>
	</form:form>
</section>
</c:if>
