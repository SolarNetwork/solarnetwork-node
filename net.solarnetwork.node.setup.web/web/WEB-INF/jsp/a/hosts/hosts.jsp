<section class="intro clearfix">
	<p><fmt:message key="hosts.intro"/></p>
</section>
<div class="init">
	<div class="progress progress-striped active">
		<div class="bar" style="width: 100%;"></div>
    </div>
</div>
<div class="ready hidden">
	<div class="row">
		<button type="button" class="btn btn-primary pull-right" data-toggle="modal" data-target="#host-add-modal">
			<i class="fas fa-plus"></i> <fmt:message key="host.add.action"/>
		</button>
	</div>
	<section id="hosts">
		<div class="row template hbox" style="align-items: center;">
			<div class="span8">
				<button class="btn btn-link edit-item" data-tprop="name">myhost</button>
			</div>
			<div class="span4">
				<span data-tprop="address">192.168.1.2</span>
			</div>
		</div>
		<div class="list-content"></div>
	</section>

</div>

<form id="host-add-modal" class="modal hide fade" action="<setup:url value='/a/hosts/add'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key="host.add.title"/></h3>
	</div>
	<div class="modal-body before">
		<p><fmt:message key="host.add.intro"/></p>
		<label for="hosts-add-name"><fmt:message key="host.name.label"/></label>
		<input type="text" class="input-block-level" maxlength="256" name="name" id="hosts-add-name"
			required placeholder="<fmt:message key='host.name.placeholder'/>">
		<label for="hosts-add-address"><fmt:message key="host.address.label"/></label>
		<input type="text" class="input-block-level" maxlength="256" name="address" id="hosts-add-address"
			required placeholder="<fmt:message key='host.address.placeholder'/>">
	</div>
	<div class="modal-body after hidden">
		<p class="error hidden"><fmt:message key="host.add.error"/></p>
	</div>
	<div class="modal-footer">
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<button type="submit" class="btn btn-primary ladda-button expand-right before"
				data-loading-text="<fmt:message key='host.adding.message'/>"><fmt:message key="host.add.button"/></button>
	</div>
	<sec:csrfInput/>
</form>

<form id="host-remove-modal" class="modal hide fade" action="<setup:url value='/a/hosts/remove'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key="host.remove.title"/></h3>
	</div>
	<div class="modal-body before">
		<p><fmt:message key="host.remove.intro"/></p>
	</div>
	<div class="modal-body after hidden">
		<p class="error hidden"><fmt:message key="host.remove.error"/></p>
	</div>
	<div class="modal-footer">
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<button type="submit" class="btn btn-danger ladda-button expand-right before"
				data-loading-text="<fmt:message key='host.removing.message'/>"><fmt:message key="host.remove.button"/></button>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="name">
</form>