<section class="my-3">
	<setup:url value="/associate/restoreBackup" var="action"/>
	<form:form action="${action}" method="post" id="associate-restore-backup-form">
		<p><fmt:message key="node.setup.restore.imported.intro"/></p>	
		<fieldset id="associate-restore-list-container" class="menu-list noselect"
			data-msg-items="<fmt:message key='items'/>" data-msg-item="<fmt:message key='item'/>"></fieldset>
		<div class="progress hidden" role="progressbar">
			<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
	   	</div>		
		<div class="row mt-3">
			<div class="d-grid">
				<button type="submit" class="btn btn-primary">
					<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
					<span role="status"><fmt:message key='node.setup.restore.restore'/></span>
				</button>
			</div>
		</div>
		<sec:csrfInput/>
	</form:form>
</section>