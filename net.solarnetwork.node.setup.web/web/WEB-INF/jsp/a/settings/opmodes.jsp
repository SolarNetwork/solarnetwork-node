<section class="intro">
	<h2>
		<fmt:message key="opmodes.title"/>
	</h2>
	<p>
		<fmt:message key="opmodes.intro"/>
	</p>
</section>
<section id="opmodes">
	<div class="d-grid justify-content-end">
		<button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#add-opmodes-modal">
			<i class="bi bi-plus-lg"></i>
			<span><fmt:message key='opmodes.action.add'/></span>
		</button>
	</div>
	<p class="opmodes-none"><fmt:message key="opmodes.noActive.intro"/></p>
	<table class="table table-condensed table-hover hidden opmodes-some" id="opmodes-active">
		<thead>
			<tr>
				<th><fmt:message key="opmodes.mode.label"/></th>
				<th></th>
			</tr>
			<tr class="template item">
				<td data-tprop="mode" class="align-middle"></td>
				<td>
					<button type="button" class="btn btn-danger" name="delete" title="<fmt:message key='opmodes.action.delete'/>">
						<i class="bi bi-trash3"></i>
					</button>
				</td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<form id="add-opmodes-modal" class="modal fade dynamic" data-bs-backdrop="static" data-bs-keyboard="false" action="<setup:url value='/a/opmodes/active'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="opmodes.add.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key="opmodes.add.intro"/></p>
				<label class="form-label" for="opmodes-modes"><fmt:message key="opmodes.modes.label"/></label>
				<input class="form-control" id="opmodes-modes" type="text" maxlength="512" name="modes">
			</div>
			<div class="modal-footer">
				<sec:csrfInput/>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="opmodes.add.label"/></button>
			</div>
		</div>
	</div>
</form>
