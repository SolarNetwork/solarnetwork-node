<section class="intro">
	<h2><fmt:message key="locstate.title"/></h2>
	<p><fmt:message key="locstate.intro"/></p>
	<div class="d-flex justify-content-end">
		<button type="button" class="btn btn-primary" data-bs-target="#local-state-edit-modal" data-bs-toggle="modal">
			<i class="bi bi-plus-lg"></i>
			<fmt:message key="locstate.action.create"/>
		</button>
	</div>
</section>
<section class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</section>
<section id="local-state" class="hidden ready">
	<p class="none"><fmt:message key="locstate.intro.none"/></p>
	<table class="table table-sm some hidden" id="locstate-list">
		<thead>
			<tr>
				<th><fmt:message key="locstate.entity.key.label"/></th>
				<th><fmt:message key="locstate.entity.created.label"/></th>
				<th><fmt:message key="locstate.entity.modified.label"/></th>
				<th><fmt:message key="locstate.entity.type.label"/></th>
				<th><fmt:message key="locstate.entity.value.label"/></th>
			</tr>
			<tr class="template item">
				<td><a href="#" class="edit-link" data-tprop="key"></a></td>
				<td data-tprop="createdDate"></td>
				<td data-tprop="modifiedDate"></td>
				<td data-tprop="typeDisplay"></td>
				<td>
					<span data-tprop="valueDisplay"></span>
				</td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<form id="local-state-edit-modal" class="modal modal-lg fade" action="<setup:url value='/a/local-state'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='locstate.edit.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='locstate.edit.intro'/></p>
				<div class="col-12 mb-3">
					<label class="form-label" for="edit-local-state-modal-key">
						<fmt:message key="locstate.entity.key.label"/>
					</label>
					<input type="text" name="key" id="edit-local-state-modal-key"
						class="form-control" value=""/>
				</div>
				<div class="col-12 mb-3">
					<label class="form-label" for="edit-local-state-modal-type">
						<fmt:message key="locstate.entity.type.label"/>
					</label>
					<select name="type" id="edit-local-state-modal-name" class="form-control">
						<option value=""><fmt:message key="locstate.entity.type.auto"/></option>
						<option value="Boolean"><fmt:message key="locstate.entity.type.Boolean"/></option>
						<option value="Integer"><fmt:message key="locstate.entity.type.Integer"/></option>
						<option value="Int32"><fmt:message key="locstate.entity.type.Int32"/></option>
						<option value="Int64"><fmt:message key="locstate.entity.type.Int64"/></option>
						<option value="Integer"><fmt:message key="locstate.entity.type.Integer"/></option>
						<option value="Decimal"><fmt:message key="locstate.entity.type.Decimal"/></option>
						<option value="Float32"><fmt:message key="locstate.entity.type.Float32"/></option>
						<option value="Float64"><fmt:message key="locstate.entity.type.Float64"/></option>
						<option value="String"><fmt:message key="locstate.entity.type.String"/></option>
						<option value="Mapping"><fmt:message key="locstate.entity.type.Mapping"/></option>
					</select>
					<div class="form-text">
						<fmt:message key="locstate.type.help"/>
					</div>
				</div>
				<div class="col-12 mb-3">
					<label class="form-label" for="edit-local-state-modal-value">
						<fmt:message key="locstate.entity.value.label"/>
					</label>
					<textarea name="value" id="edit-local-state-modal-value"
						class="form-control" maxLength="8000"></textarea>
				</div>
			</div>
			<div class="modal-footer justify-content-between">
				<button type="button" class="btn btn-danger pull-left" name="delete" title="<fmt:message key='locstate.action.delete'/>">
					<i class="bi bi-trash3"></i>
					<fmt:message key="locstate.action.delete"/>
				</button>
				<div>
					<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></button>
					<button type="submit" class="btn btn-primary"><fmt:message key="locstate.action.save"/></button>
				</div>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>
