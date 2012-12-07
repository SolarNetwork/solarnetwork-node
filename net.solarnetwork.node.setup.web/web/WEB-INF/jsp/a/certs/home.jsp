<h2><fmt:message key='certs.home.title'/></h2>
<p><fmt:message key='certs.home.intro'/></p>

<table class="table">
	<thead>
		<tr>
			<th><fmt:message key='certs.status.label'/></th>
			<th><fmt:message key='certs.subject.label'/></th>
			<th><fmt:message key='certs.expiration.label'/></th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>
				<c:choose>
					<c:when test="${nodeCertExpired}">
						<span class="label label-important"><fmt:message key='certs.status.expired'/></span>
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
					${nodeCert.subjectDN.name}<br/>
					<small class="muted span6">
						<fmt:message key='certs.subject.issuedby'/>
						<em> ${nodeCert.issuerDN.name}</em>
					</small>
				</c:if>
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
	<div class="span8">
		<a class="btn" id="btn-view-node-csr" href="<c:url value='/certs/nodeCSR'/>">
			<fmt:message key='certs.action.csr'/>
		</a>
		<a class="btn${nodeCertValid ? '' : ' btn-primary'}" id="btn-view-node-csr" href="#import-cert-modal" data-toggle="modal">
			<fmt:message key='certs.action.import'/>
		</a>
		<a class="btn${nodeCertValid ? ' btn-primary' : ''}" id="btn-export-node-cert" href="<c:url value='/certs/nodeCert'/>">
			<fmt:message key='certs.action.export'/>
		</a>
	</div>
</div>

<form id="import-cert-modal" class="modal hide fade" action="<c:url value='/certs/import'/>" method="post" enctype="multipart/form-data">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key='certs.import.title'/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key='certs.import.intro'/></p>
		<label class="control-label" for="import-cert-file">
			<fmt:message key='certs.import.file.label'/>
		</label>
		<input class="span3" id="import-cert-file" type="file" name="file"/>
		<br/><br/>
		<div class="alert alert-info"><fmt:message key='certs.import.xor'/></div>

		<label class="control-label" for="import-cert-text">
			<fmt:message key='certs.import.text.label'/>
		</label>		
		<textarea name="text" id="import-cert-text" rows="6" class="span6 cert" 
				placeholder="<fmt:message key='certs.import.text.placeholder'/>"></textarea>
	</div>
	<div class="modal-footer">
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
		<button type="submit" class="btn btn-primary"><fmt:message key="certs.action.import"/></button>
	</div>
</form>

<div id="view-csr-modal" class="modal dynamic hide fade">
 	<div class="modal-header">
 		<button type="button" class="close" data-dismiss="modal">&times;</button>
 		<h3><fmt:message key='certs.csr.title'/></h3>
 	</div>
 	<div class="modal-body">
 		<p><fmt:message key='certs.csr.intro'/></p>
 		<pre class="cert" id="modal-csr-container"></pre>
 	</div>
 	<div class="modal-footer">
 		<a href="#" class="btn btn-primary" data-dismiss="modal"><fmt:message key='close.label'/></a>
 	</div>
</div>

<form id="export-cert-modal" class="modal dynamic hide fade" action="<c:url value='/certs/nodeCert'/>" method="get">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key='certs.export.title'/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key='certs.export.intro'/></p>

		<pre class="cert" id="modal-cert-container"></pre>
		
		<label class="checkbox">
			<input type="checkbox" name="chain" value="true"/>
			<fmt:message key='certs.export.chain.label'/>
		</label>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="download" value="true"/>
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
		<button type="submit" class="btn btn-primary"><fmt:message key="certs.action.export"/></button>
	</div>
</form>

