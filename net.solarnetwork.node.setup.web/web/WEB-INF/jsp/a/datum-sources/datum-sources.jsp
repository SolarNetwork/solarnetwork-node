<section class="intro">
	<h2><fmt:message key="datum-sources.title"/></h2>
	<p><fmt:message key="datum-sources.intro"/></p>
</section>
<section class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</section>
<section id="datum-sources" class="hidden ready">
	<p class="none"><fmt:message key="datum-sources.intro.none"/></p>
	<table class="table table-sm some hidden" id="datum-sources-list">
		<thead>
			<tr>
				<th><fmt:message key="datum-source.sourceId"/></th>
				<th><fmt:message key="datum-source.name"/></th>
				<th><fmt:message key="datum-source.uid"/></th>
				<th><fmt:message key="datum-source.lastPublishDate"/></th>
			</tr>
			<tr class="template item">
				<td><code><a href="#" class="datum-source-link" data-tprop="sourceId"></a></code></td>
				<td data-tprop="displayName"></td>
				<td data-tprop="uid"></td>
				<td><button type="button" class="btn btn-link p-0 hidden datum-source-details-link"></button></td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<div id="datum-sources-datum-detail-modal" class="modal modal-lg fade dynamic" data-bs-backdrop="static" data-bs-keyboard="false">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="datum-sources.datum.details.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key="datum-sources.datum.details.intro"/></p>
				<table class="table table-sm some">
					<thead>
						<tr>
							<th><fmt:message key="datum.property.label"/></th>
							<th><fmt:message key="datum.property.value.label"/></th>
						</tr>
						<tr class="template item">
							<th data-tprop="propName"></th>
							<td data-tprop="propVal"></td>
						</tr>
					</thead>
					<tbody class="list-container">
					</tbody>
				</table>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
			</div>
		</div>
	</div>
</div>
