<section class="intro">
	<h2>
		<fmt:message key="settings.factory.title">
			<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
		</fmt:message>
	</h2>
	<c:set var="serviceDescription">
		<setup:message key="desc" messageSource="${factory.messageSource}" text=""/>
	</c:set>
	<c:if test="${fn:length(serviceDescription) > 0}">
		<p class="lead">${serviceDescription}</p>
	</c:if>
	<p>
		<fmt:message key="settings.factory.intro">
			<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
		</fmt:message>
	</p>
	<p>
		<a href="<setup:url value='/a/settings'/>" class="btn">
			<i class="icon-arrow-left"></i>
			<fmt:message key="back.label"/>
		</a>
		<button type="button" class="btn btn-primary" id="add">
			<i class="icon-plus icon-white"></i>
			<fmt:message key='settings.factory.add'>
				<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
			</fmt:message>
		</button>
	</p>
</section>

<section id="settings">
	<form class="form-horizontal" action="<setup:url value='/a/settings/save'/>" method="post">
		<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
			<c:set var="instance" value="${instance}" scope="request"/>
			<c:forEach items="${instance.value}" var="provider" varStatus="providerStatus">
				<c:set var="provider" value="${provider}" scope="request"/>
				<c:set var="instanceId" value="${provider.factoryInstanceUID}" scope="request"/>
				<!--  ${provider.settingUID} -->
		
					<fieldset>
						<legend>
							<setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/>
							${' '}
							${instance.key}
						</legend>
						
						<c:catch var="providerException">		
							<c:forEach items="${provider.settingSpecifiers}" var="setting" varStatus="settingStatus">
								<c:set var="setting" value="${setting}" scope="request"/>
								<c:set var="settingId" value="m${instanceStatus.index}s${providerStatus.index}i${settingStatus.index}" scope="request"/>
								<c:import url="/WEB-INF/jsp/a/settings/setting-control.jsp"/>
							</c:forEach>
						</c:catch>
						<c:if test="${not empty providerException}">
							<div class="alert alert-warning">
								<fmt:message key="settings.error.provider.exception">
									<fmt:param value="${providerException.cause.message}"/>
								</fmt:message>
							</div>
						</c:if>
						<div class="control-group">
							<div class="controls">
								<button type="button" class="btn btn-danger" id="del${instance.key}">
									<fmt:message key='settings.factory.delete'>
										<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
										<fmt:param value="${instance.key}"/>
									</fmt:message>
								</button>
								<script>
								$('#del${instance.key}').click(function() {
									SolarNode.Settings.deleteFactoryConfiguration({
										button: this,
										url: '<setup:url value="/a/settings/manage/delete"/>',
										factoryUID: '${factory.factoryUID}',
										instanceUID: '${instance.key}'
									});
								});
								</script>
								<button type="button" class="btn btn-danger" id="reset${instance.key}">
									<fmt:message key='settings.factory.reset'>
										<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
										<fmt:param value="${instance.key}"/>
									</fmt:message>
								</button>
								<script>
								$('#reset${instance.key}').click(function() {
									SolarNode.Settings.resetFactoryConfiguration({
										button: this,
										url: '<setup:url value="/a/settings/manage/reset"/>',
										factoryUID: '${factory.factoryUID}',
										instanceUID: '${instance.key}'
									});
								});
								</script>
							</div>
						</div>
					</fieldset>
			</c:forEach>
		</c:forEach>
		<div class="form-actions">
			<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
		</div>
		<sec:csrfInput/>
	</form>
</section>
<script>
$(function() {
	$('#submit').click(function() {
		SolarNode.Settings.saveUpdates($(this.form).attr('action'), {
			success: '<fmt:message key="settings.save.success.msg"/>',
			error: '<fmt:message key="settings.save.error.msg"/>',
			title: '<fmt:message key="settings.save.result.title"/>',
			button: '<fmt:message key="ok.label"/>'
		});
	});
	$('#add').click(function() {
		SolarNode.Settings.addFactoryConfiguration({
			button: this,
			url: '<setup:url value="/a/settings/manage/add"/>',
			factoryUID: '${factory.factoryUID}'
		});
	});
	SolarNode.Settings.reset();
});
</script>
<div id="alert-delete" class="alert alert-danger alert-block hidden">
	<button type="button" class="close" data-dismiss="alert">×</button>
	<h4><fmt:message key="settings.factory.delete.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.delete.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="delete.label"/>
	</button>
</div>
<div id="alert-reset" class="alert alert-danger alert-block hidden">
	<button type="button" class="close" data-dismiss="alert">×</button>
	<h4><fmt:message key="settings.factory.reset.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.reset.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="reset.label"/>
	</button>
</div>
<form class="modal dynamic hide fade lookup-modal sn-loc-lookup-modal price-lookup-modal" 
		action="<setup:url value='/api/v1/sec/location'/>" method="get">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key='lookup.price.title'/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key='lookup.price.intro'/></p>
		<div class="form-inline">
			<input type="hidden" name="tags" value="price"/>
			<input type="text" class="span4" maxlength="64" name="query" placeholder="<fmt:message key='lookup.price.search.placeholder'/>"/>
			<button type="submit" class="btn btn-primary ladda-button expand-right" data-loading-text="<fmt:message key='lookup.searching.label'/>">
				<fmt:message key='lookup.action.search'/>
			</button>
		</div>

		<table class="table table-striped table-hover hidden search-results">
			<thead>
				<tr>
					<th><fmt:message key='lookup.price.sourceName'/></th>
					<th><fmt:message key='lookup.price.locationName'/></th>
					<th><fmt:message key='lookup.price.country'/></th>
					<th><fmt:message key='lookup.price.region'/></th>
					<th><fmt:message key='lookup.price.currency'/></th>
				</tr>
				<tr class="template">
					<td data-tprop="sourceId"></td>
					<td data-tprop="m.name"></td>
					<td data-tprop="location.country"></td>
					<td data-tprop="location.region"></td>
					<td data-tprop="m.currency"></td>
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>
		
		<label id="price-lookup-selected-label" class="hidden">
			<fmt:message key='lookup.selected.label'/>
			<span id="price-lookup-selected-container"></span>
		</label>
	</div>
	<div class="modal-footer">
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
		<button type="button" class="btn btn-primary choose" disabled="disabled">
			<fmt:message key="lookup.action.choose"/>
		</button>
	</div>
</form>
<form class="modal dynamic hide fade lookup-modal sn-loc-lookup-modal weather-lookup-modal day-lookup-modal" 
		action="<setup:url value='/api/v1/sec/location'/>" method="get">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key='lookup.weather.title'/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key='lookup.weather.intro'/></p>
		<div class="form-inline">
			<input type="hidden" name="tags" value="weather"/>
			<input type="text" class="span4" maxlength="64" name="location.region" placeholder="<fmt:message key='lookup.weather.search.placeholder'/>"/>
			<button type="submit" class="btn btn-primary ladda-button expand-right" 
				data-loading-text="<fmt:message key='lookup.searching.label'/>">
				<fmt:message key='lookup.action.search'/>
			</button>
		</div>

		<table class="table table-striped table-hover hidden search-results">
			<thead>
				<tr>
					<th><fmt:message key='lookup.weather.sourceName'/></th>
					<th><fmt:message key='lookup.weather.country'/></th>
					<th><fmt:message key='lookup.weather.region'/></th>
					<th><fmt:message key='lookup.weather.locality'/></th>
					<th><fmt:message key='lookup.weather.postalCode'/></th>
				</tr>
				<tr class="template">
					<td data-tprop="sourceId"></td>
					<td data-tprop="location.country"></td>
					<td data-tprop="location.region"></td>
					<td data-tprop="location.locality"></td>
					<td data-tprop="location.postalCode"></td>
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>
		
		<label id="weather-lookup-selected-label" class="hidden">
			<fmt:message key='lookup.selected.label'/>
			<span id="weather-lookup-selected-container"></span>
		</label>
	</div>
	<div class="modal-footer">
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
		<button type="button" class="btn btn-primary choose" disabled="disabled">
			<fmt:message key="lookup.action.choose"/>
		</button>
	</div>
</form>
