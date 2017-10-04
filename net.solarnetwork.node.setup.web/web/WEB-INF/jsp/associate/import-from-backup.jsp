<%--
	Expects the following request-or-higher properties:
	
	settingsService 		- the SettingService
	backupManager 		- the BackupManager
--%>
<p><fmt:message key="node.setup.restore.intro"/></p>

<setup:url value="/associate/importBackup" var="action"/>
<form:form action="${action}" method="post" cssClass="form-horizontal" enctype="multipart/form-data" id="associate-import-backup-form">
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="restore-file">
				<fmt:message key="node.setup.restore.file"/>
			</label>
			<div class="controls">
				<input class="span3" type="file" name="file" id="restore-file"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<button type="submit" class="btn btn-primary ladda-button expand-right"><fmt:message key='node.setup.restore.upload'/></button>
	</div>
	<sec:csrfInput/>
</form:form>

<c:if test="${not empty backupManager}">
<p><fmt:message key="node.setup.restore.backup.intro"/></p>
<setup:url value="/associate/chooseBackup" var="action"/>
<form:form action="${action}" method="post" cssClass="form-horizontal" id="associate-choose-backup-form">
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

	<fieldset style="margin-top: 24px;">
		<div class="control-group">
			<label class="control-label" for="backup-backups">
				<fmt:message key="backup.backups.label"/>
			</label>
			<div class="controls">
				<select name="backup" class="span3" id="backup-backups">
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

	<div class="form-actions">
		<button type="button" class="btn btn-info" id="associate-choose-backup-apply-settings"><fmt:message key='node.setup.restore.backup.settings.save'/></button>
		<button type="submit" class="btn btn-primary ladda-button expand-right"><fmt:message key="backup.restore.button"/></button>
	</div>
	<sec:csrfInput/>
</form:form>
</c:if>
