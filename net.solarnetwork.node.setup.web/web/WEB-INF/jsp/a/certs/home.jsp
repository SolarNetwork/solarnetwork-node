<h2><fmt:message key='certs.home.title'/></h2>
<p><fmt:message key='certs.home.intro'/></p>
<jsp:useBean class="java.util.Date" id="now"/>
<div class="row form-horizontal">
	<label class="span2" for=""><fmt:message key='certs.status.label'/></label>
	<div class="span1">
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
	</div>
	<div class="span3">
		<a class="btn" id="btn-view-node-csr" href="<c:url value='/certs/nodeCSR'/>">
			<fmt:message key='certs.action.csr'/>
		</a>
	</div>
</div>
<div id="view-cert-modal" class="modal dynamic hide fade">
 	<div class="modal-header">
 		<button type="button" class="close" data-dismiss="modal">&times;</button>
 		<h3><fmt:message key='certs.csr.title'/></h3>
 	</div>
 	<div class="modal-body">
 		<p><fmt:message key='certs.csr.intro'/></p>
 		<pre class="cert" id="modal-cert-container"></pre>
 	</div>
 	<div class="modal-footer">
 		<a href="#" class="btn btn-primary" data-dismiss="modal"><fmt:message key='close.label'/></a>
 	</div>
</div>
