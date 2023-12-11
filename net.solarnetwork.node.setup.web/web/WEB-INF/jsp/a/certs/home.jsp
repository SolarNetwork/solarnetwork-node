<h2><fmt:message key='certs.home.title'/></h2>
<p><fmt:message key='certs.home.intro'/></p>

<table class="table">
	<thead>
		<tr>
			<th><fmt:message key='certs.status.label'/></th>
			<th><fmt:message key='certs.subject.label'/></th>
			<th><fmt:message key='certs.serialNumber.label'/></th>
			<th><fmt:message key='certs.expiration.label'/></th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>
				<c:choose>
					<c:when test="${nodeCertExpired}">
						<span class="label label-danger"><fmt:message key='certs.status.expired'/></span>
					</c:when>
					<c:when test="${nodeCertValid}">
						<span class="label label-success"><fmt:message key='certs.status.valid'/></span>
					</c:when>
					<c:otherwise>
						<span class="label label-warning"><fmt:message key='certs.status.pending'/></span>
					</c:otherwise>
				</c:choose>
			</td>
			<td>
				<c:if test="${not empty nodeCert}">
					${nodeCert.subjectDN}<br/>
					<small class="test-muted col-md-6">
						<fmt:message key='certs.subject.issuedby'/>
						<em> ${nodeCert.issuerDN}</em>
					</small>
				</c:if>
			</td>
			<td>
				${nodeCertSerialNumber}
			</td>
			<td>
				<c:if test="${nodeCertValid}">
					<fmt:formatDate value="${nodeCert.notAfter}" pattern="dd MMM yyyy HH:mm" timeZone="GMT"/> GMT
				</c:if>
			</td>
		</tr>
	</tbody>
</table>

<div class="row">
	<div class="col-md-12">
		<a class="btn btn-secondary" id="btn-view-node-csr" href="<setup:url value='/a/certs/nodeCSR'/>">
			<fmt:message key='certs.action.csr'/>
		</a>
		<a class="btn ${nodeCertValid ? 'btn-secondary' : 'btn-primary'}" id="btn-view-node-csr" href="#import-cert-modal" data-bs-toggle="modal">
			<fmt:message key='certs.action.import'/>
		</a>
		<c:if test="${not empty nodeCert}">
			<a class="btn btn-secondary" id="btn-renew-node-cert" href="<setup:url value='/a/certs/renew'/>">
				<fmt:message key='certs.action.renew'/>
			</a>
		</c:if>
		<a class="btn ${nodeCertValid ? 'btn-primary' : 'btn-secondary'}" id="btn-export-node-cert" href="<setup:url value='/a/certs/nodeCert'/>">
			<fmt:message key='certs.action.view'/>
		</a>
	</div>
</div>

<form id="import-cert-modal" class="modal hide fade" action="<setup:url value='/a/certs/import'/>" method="post" enctype="multipart/form-data">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='certs.import.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='certs.import.intro'/></p>
				<div class="col-12 mb-3">
					<label class="form-label" for="import-cert-file">
						<fmt:message key='certs.import.file.label'/>
					</label>
					<input class="form-control" id="import-cert-file" type="file" name="file"/>
				</div>

				<div class="alert alert-info mb-3"><fmt:message key='certs.import.xor'/></div>
		
				<div class="col-12">
					<label class="form-label" for="import-cert-text">
						<fmt:message key='certs.import.text.label'/>
					</label>
					<textarea name="text" id="import-cert-text" rows="6" class="form-control cert"
							placeholder="<fmt:message key='certs.import.text.placeholder'/>"></textarea>
				</div>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="certs.action.import"/></button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<div id="view-csr-modal" class="modal dynamic hide fade">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<h3 class="modal-title"><fmt:message key='certs.csr.title'/></h3>
		 		<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
		 	</div>
		 	<div class="modal-body">
		 		<p><fmt:message key='certs.csr.intro'/></p>
		 		<pre class="cert" id="modal-csr-container"></pre>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-primary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
		 	</div>
		</div>
	</div>
</div>

<form id="export-cert-modal" class="modal dynamic hide fade" action="<setup:url value='/a/certs/nodeCert'/>" method="get">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='certs.export.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='certs.export.intro'/></p>
		
				<pre class="cert" id="modal-cert-container"></pre>
		
				<div class="form-check">
					<input class="form-check-input" type="checkbox" name="chain" value="true" id="export-cert-chain">
					<label class="form-check-label" for="export-cert-chain"><fmt:message key='certs.export.chain.label'/></label>
				</div>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="certs.action.export"/></button>
			</div>
		</div>
	</div>
	<input type="hidden" name="download" value="true"/>
</form>

<form id="renew-cert-modal" class="modal dynamic hide fade" action="<setup:url value='/a/certs/renew'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='certs.renew.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body start">
				<p><fmt:message key='certs.renew.intro'/></p>
		
				<div class="col-12 mb-3">
					<label class="form-label" for="renew-cert-password">
						<fmt:message key='certs.renew.password.label'/>
					</label>
					<input class="form-control" id="renew-cert-password" type="password" name="password" required="required"/>
				</div>
		
				<div class="col-12 mb-3">
					<label class="form-label" for="renew-cert-password-again">
						<fmt:message key='certs.renew.password-again.label'/>
					</label>
					<input class="form-control" id="renew-cert-password-again" type="password" name="passwordAgain" required="required"/>
				</div>
				
				<div class="alert alert-danger" id="renew-cert-error-password-again" style="display: none;"><fmt:message key='certs.renew.password.mismatch'/></div>
			</div>
			<div class="modal-body success" style="display: none;">
				<p><fmt:message key='certs.renew.success'/></p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary start"><fmt:message key="certs.action.renew"/></button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>
