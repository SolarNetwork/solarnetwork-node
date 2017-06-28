<setup:url value="/associate/restoreBackup" var="action"/>
<form:form action="${action}" method="post" cssClass="form-horizontal span8" id="associate-restore-backup-form">
	<p><fmt:message key="node.setup.restore.imported.intro"/></p>	
	<fieldset id="associate-restore-list-container" class="menu-list noselect"
		data-msg-items="<fmt:message key='items'/>" data-msg-item="<fmt:message key='item'/>"></fieldset>
	<div class="progress progress-striped active hide">
     	<div class="bar" style="width: 100%;"></div>
   	</div>		
	<div class="form-actions">
		<button type="submit" class="btn btn-primary ladda-button expand-right"><fmt:message key='node.setup.restore.restore'/></button>
	</div>
	<sec:csrfInput/>
</form:form>
