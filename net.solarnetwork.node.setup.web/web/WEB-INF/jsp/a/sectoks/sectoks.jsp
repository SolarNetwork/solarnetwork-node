<%--
	Inputs:

	tokens 				- list of SecurityToken instances
 --%>
<section class="intro">
	<h2><fmt:message key="sectoks.title"/></h2>
	<p>
		<fmt:message key="sectoks.intro">
			<fmt:param value="${fn:length(tokens)}"></fmt:param>
		</fmt:message>
	</p>
	<div class="d-flex justify-content-end">
		<button type="button" class="btn btn-primary" data-bs-target="#create-security-token-modal" data-bs-toggle="modal">
			<i class="bi bi-plus-lg"></i>
			<fmt:message key="sectoks.action.create"/>
		</button>
	</div>
</section>
<section id="sectoks">
	<c:if test="${fn:length(tokens) > 0}">
		<table class="table">
			<thead>
				<tr>
					<th><fmt:message key="sectoks.token.id.label"/></th>
					<th><fmt:message key="sectoks.token.created.label"/></th>
					<th><fmt:message key="sectoks.token.name.label"/></th>
					<th><fmt:message key="sectoks.token.description.label"/></th>
				</tr>
			</thead>
			<tbody>
			<c:forEach items="${tokens}" var="token">
				<tr>
					<td><a href="#edit-security-token-modal" class="edit-link"
						data-token-id="${token.id}"
						data-token-name="${token.name}"
						data-token-description="${token.description}"
						>${token.id}</a></td>
					<td><fmt:formatDate type="date" value="${token.createdDate}"/></td>
					<td>${token.name}</td>
					<td>${token.description}</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</c:if>
</section>

<form id="create-security-token-modal" class="modal fade" action="<setup:url value='/a/security-tokens'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='sectoks.create.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body before">
				<p><fmt:message key='sectoks.create.intro'/></p>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-name">
						<fmt:message key="sectoks.token.name.label"/>
					</label>
					<input type="text" name="name" id="create-security-token-modal-name"
						class="form-control" maxLength="128" value="" />
				</div>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-description">
						<fmt:message key="sectoks.token.description.label"/>
					</label>
					<input type="text" name="description" id="create-security-token-modal-description"
						class="form-control" maxLength="256" value="" />
				</div>
			</div>
			<div class="modal-body after hidden">
				<p class="alert alert-success"><fmt:message key='sectoks.created.intro'/></p>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-id">
						<fmt:message key="sectoks.token.id.label"/>
					</label>
					<input type="text" name="tokenId" id="create-security-token-modal-id"
						class="form-control" value="" readonly />
				</div>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-secret">
						<fmt:message key="sectoks.token.secret.label"/>
					</label>
					<input type="text" name="tokenSecret" id="create-security-token-modal-secret"
						class="form-control" value="" readonly />
				</div>
				<p class="text-center">
					<a class="btn btn-success btn-lg" href="#" id="create-token-download-csv" download="node-token-credentials.csv">
						<i class="bi bi-download"></i>
						<fmt:message key="sectoks.action.csvdownload"/>
					</a>
				</p>
				<p class="alert alert-warning"><fmt:message key='sectoks.created.warning'/></p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="sectoks.action.create"/></button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<form id="edit-security-token-modal" class="modal fade" action="<setup:url value='/a/security-tokens'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='sectoks.edit.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='sectoks.edit.intro'/></p>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-id">
						<fmt:message key="sectoks.token.id.label"/>
					</label>
					<input type="text" name="id" id="create-security-token-modal-id"
						class="form-control" value="" readonly />
				</div>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-name">
						<fmt:message key="sectoks.token.name.label"/>
					</label>
					<input type="text" name="name" id="create-security-token-modal-name"
						class="form-control" maxLength="128" value="" />
				</div>
				<div class="col-12 mb-3">
					<label class="form-label" for="create-security-token-modal-description">
						<fmt:message key="sectoks.token.description.label"/>
					</label>
					<input type="text" name="description" id="create-security-token-modal-description"
						class="form-control" maxLength="256" value="" />
				</div>
			</div>
			<div class="modal-footer justify-content-between">
				<button type="button" class="btn btn-danger pull-left" name="delete" title="<fmt:message key='sectoks.action.delete'/>">
					<i class="bi bi-trash3"></i>
					<fmt:message key="sectoks.action.delete"/>
				</button>
				<div>
					<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></button>
					<button type="submit" class="btn btn-primary"><fmt:message key="sectoks.action.update"/></button>
				</div>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>
