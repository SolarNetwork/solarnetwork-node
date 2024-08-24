<form id="edit-setting-note-modal" class="modal fade dynamic" action="<setup:url value='/a/settings/notes'/>" method="post"
		data-bs-backdrop="static" data-bs-keyboard="false">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="settings.note.edit.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key="settings.note.edit.intro"/></p>
				<label class="form-label" for="edit-setting-note-help" id="edit-setting-note-name"></label>
				<div class="card card-body bg-light" id="edit-setting-note-help">
					<span class="form-text"></span>
				</div>

				<label class="form-label mt-3" for="edit-setting-note-note"><fmt:message key="note.label"/></label>
				<textarea id="edit-setting-note-note" name="values[0].note" class="form-control" maxlength="4096"></textarea>
			</div>
			<div class="modal-footer">
				<sec:csrfInput/>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="settings.note.edit.save.label"/></button>
			</div>
		</div>
	</div>
	<input type="hidden" id="edit-setting-note-provider" name="values[0].providerKey" value="">
	<input type="hidden" id="edit-setting-note-instance" name="values[0].instanceKey" value="">
	<input type="hidden" id="edit-setting-note-key" name="values[0].key" value="">
</form>
