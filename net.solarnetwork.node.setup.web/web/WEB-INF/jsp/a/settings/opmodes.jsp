<section class="intro">
	<h2>
		<fmt:message key="opmodes.title"/>
	</h2>
	<p>
		<fmt:message key="opmodes.intro"/>
	</p>
</section>
<section id="opmodes">
	<div>
		<button type="button" class="btn btn-primary pull-right" data-bs-toggle="modal" data-bs-target="#add-opmodes-modal">
			<i class="fas fa-plus"></i>
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
				<td data-tprop="mode"></td>
				<td>
					<button type="button" class="btn btn-danger" name="delete" title="<fmt:message key='opmodes.action.delete'/>">
						<i class="far fa-trash-can"></i>
					</button>
				</td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<form id="add-opmodes-modal" class="modal dynamic hide fade" data-backdrop="static" action="<setup:url value='/a/opmodes/active'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3>
			<fmt:message key="opmodes.add.title"/>
		</h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key="opmodes.add.intro"/></p>
		<label for="opmodes-modes"><fmt:message key="opmodes.modes.label"/></label>
		<input type="text" class="form-control" maxlength="512" name="modes">
	</div>
	<div class="modal-footer">
		<sec:csrfInput/>
		<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		<button type="submit" class="btn btn-primary"><fmt:message key="opmodes.add.label"/></button>
	</div>
</form>
