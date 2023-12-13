<section class="intro clearfix">
	<p><fmt:message key="hosts.intro"/></p>
</section>
<div class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</div>
<div class="ready hidden">
	<div class="row justify-content-end">
		<div class="col-auto">
			<button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#host-add-modal">
				<i class="fas fa-plus"></i> <fmt:message key="host.add.action"/>
			</button>
		</div>
	</div>
	<section id="hosts">
		<div class="row template hbox" style="align-items: center;">
			<div class="col-md-8">
				<button class="btn btn-link edit-item" data-tprop="name">myhost</button>
			</div>
			<div class="col-md-4">
				<span data-tprop="address">192.168.1.2</span>
			</div>
		</div>
		<div class="list-content"></div>
	</section>

</div>

<form id="host-add-modal" class="modal fade" action="<setup:url value='/a/hosts/add'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">		
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="host.add.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body before">
				<p><fmt:message key="host.add.intro"/></p>
				<div class="mb-3">
					<label class="form-label" for="hosts-add-name"><fmt:message key="host.name.label"/></label>
					<input class="form-control" type="text" maxlength="256" name="name" id="hosts-add-name"
						required placeholder="<fmt:message key='host.name.placeholder'/>">
				</div>
				<div class="mb-3">
					<label class="form-label" for="hosts-add-address"><fmt:message key="host.address.label"/></label>
					<input class="form-control" type="text" maxlength="256" name="address" id="hosts-add-address"
						required placeholder="<fmt:message key='host.address.placeholder'/>">
				</div>
			</div>
			<div class="modal-body after hidden">
				<p class="error hidden"><fmt:message key="host.add.error"/></p>
			</div>
			<div class="modal-footer">
				<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></a>
				<button type="submit" class="btn btn-primary ladda-button expand-right before"
						data-loading-text="<fmt:message key='host.adding.message'/>"><fmt:message key="host.add.button"/></button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
</form>

<form id="host-remove-modal" class="modal fade" action="<setup:url value='/a/hosts/remove'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="host.remove.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body before">
				<p><fmt:message key="host.remove.intro"/></p>
			</div>
			<div class="modal-body after hidden">
				<p class="error hidden"><fmt:message key="host.remove.error"/></p>
			</div>
			<div class="modal-footer">
				<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key="close.label"/></a>
				<button type="submit" class="btn btn-danger ladda-button expand-right before"
						data-loading-text="<fmt:message key='host.removing.message'/>"><fmt:message key="host.remove.button"/></button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="name">
</form>
