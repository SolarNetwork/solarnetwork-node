<?xml version="1.0" encoding="utf-8"?>
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
	<div class="row action-progress-bar justify-content-between g-2">
		<div class="col-auto">
			<a href="<setup:url value='${navloc == "filters-component" ? "/a/settings/filters" : "/a/settings"}'/>" class="btn btn-secondary text-nowrap">
				<i class="bi bi-arrow-left"></i>
				<fmt:message key="back.label"/>
			</a>
		</div>
		<div class="col-auto">
			<div class="row g-2">
				<c:if test="${fn:length(providers) > 0}">
					<div class="col-auto">
						<button type="button" class="btn btn-secondary text-nowrap" data-bs-toggle="modal" data-bs-target="#remove-all-component-instance-modal">
							<i class="bi bi-trash3"></i>
							<fmt:message key='settings.factory.removeall.label'>
								<fmt:param>${fn:length(providers)}</fmt:param>
								<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
							</fmt:message>
						</button>
					</div>
				</c:if>
				<div class="col-auto">
					<button type="button" class="btn btn-primary text-nowrap" id="add" data-bs-toggle="modal" data-bs-target="#add-component-instance-modal">
						<i class="bi bi-plus-lg"></i>
						<fmt:message key='settings.factory.add'>
							<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
						</fmt:message>
					</button>
				</div>
			</div>
		</div>
	</div>
</section>

<section id="settings" class="carousel slide mb-5" data-interval="0">
	<form action="<setup:url value='/a/settings/save'/>" method="post">
		<c:if test="${fn:length(providers) > 0}">
			<div class="form-actions row justify-content-between align-items-baseline bg-light border-top border-bottom my-3 py-3">
				<div class="col-auto">
					<button type="button" class="btn btn-primary text-nowrap" id="submit"><fmt:message key='settings.save'/></button>
				</div>
				<div class="col">
					<c:if test="${fn:length(providers) > 1}">
						<div class="page-indicators gap-2 d-flex flex-wrap justify-content-end">
							<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
								<button type="button" data-bs-target="#settings" data-bs-slide-to="${instanceStatus.index}" data-instance-key="${instance.key}"
									class="btn ${instanceStatus.index == 0 ? 'btn-warning' : 'btn-secondary'}">${instance.key}</button>
							</c:forEach>
						</div>
					</c:if>
				</div>
			</div>
		</c:if>
		<div class="carousel-inner">
			<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
				<c:set var="instance" value="${instance}" scope="request"/>
				<c:set var="provider" value="${instance.value}" scope="request"/>
				<c:set var="instanceId" value="${provider.factoryInstanceUID}" scope="request"/>
				<!--  ${provider.settingUid} -->

				<fieldset class="carousel-item ${instanceStatus.index == 0 ? 'active' : ''}">
					<legend>
						<a id="${instance.key}"
							class="anchor"
							href="#${instance.key}"
							aria-hidden="true"><i class="bi bi-link-45deg" aria-hidden="true"></i></a>
						<setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/>
						${' '}
						${instance.key}
					</legend>
					<div class="instance-content"
							data-bs-target="<setup:url value='/a/settings/manage'/>"
							data-factory-uid="${factory.factoryUid}"
							data-instance-key="${instance.key}">
					    <div class="progress" role="progressbar">
							<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
					    </div>
					</div>
					<div class="row my-3">
						<div class="col-sm-9 offset-sm-3">
							<button type="button" class="btn btn-danger delete-factory-instance"
									data-bs-target="<setup:url value='/a/settings/manage/delete'/>"
									data-factory-uid="${factory.factoryUid}"
									data-instance-key="${instance.key}"
									>
								<fmt:message key='settings.factory.delete'>
									<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
									<fmt:param value="${instance.key}"/>
								</fmt:message>
							</button>
							<button type="button" class="btn btn-primary reset-factory-instance"
									data-bs-target="<setup:url value='/a/settings/manage/reset'/>"
									data-factory-uid="${factory.factoryUid}"
									data-instance-key="${instance.key}"
									>
								<fmt:message key='settings.factory.reset'>
									<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
									<fmt:param value="${instance.key}"/>
								</fmt:message>
							</button>
						</div>
					</div>
				</fieldset>
			</c:forEach>
		</div>
		<sec:csrfInput/>
	</form>
</section>
<script>
$(function() {
	$('#submit').on('click', function() {
		SolarNode.Settings.saveUpdates($(this.form).attr('action'), {
			success: '<fmt:message key="settings.save.success.msg"/>',
			error: '<fmt:message key="settings.save.error.msg"/>',
			title: '<fmt:message key="settings.save.result.title"/>',
			button: '<fmt:message key="ok.label"/>'
		});
	});
	SolarNode.Settings.reset();
});
</script>
<div id="alert-delete" class="alert alert-danger alert-dismissible my-3 hidden">
	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="<fmt:message key='close.label'/>"></button>
	<h4><fmt:message key="settings.factory.delete.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.delete.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="delete.label"/>
	</button>
</div>
<div id="alert-reset" class="alert alert-danger alert-dismissible my-3 hidden">
	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="<fmt:message key='close.label'/>"></button>
	<h4><fmt:message key="settings.factory.reset.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.reset.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="reset.label"/>
	</button>
</div>
<form id="remove-all-component-instance-modal" class="modal fade dynamic"
		action="<setup:url value='/a/settings/manage/removeall'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='settings.factory.removeall.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p>
					<fmt:message key='settings.factory.removeall.intro'>
						<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
					</fmt:message>
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
					<fmt:message key="cancel.label"/>
				</button>
				<button type="submit" class="btn btn-danger">
					<fmt:message key="settings.factory.removeall.remove.label"/>
				</button>
			</div>
		</div>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="uid" value="${factory.factoryUid}"/>
</form>
<form class="modal modal-lg fade dynamic lookup-modal sn-loc-lookup-modal price-lookup-modal"
		action="<setup:url value='/a/location'/>" method="get">
	<div class="modal-dialog modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='lookup.price.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='lookup.price.intro'/></p>
				<div class="input-group my-3">
					<input type="text" class="form-control" maxlength="64" name="query" placeholder="<fmt:message key='lookup.price.search.placeholder'/>"/>
					<button type="submit" class="btn btn-primary" data-loading-text="<fmt:message key='lookup.searching.label'/>">
						<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
						<span role="status"><fmt:message key='lookup.action.search'/></span>
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
				<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></a>
				<button type="button" class="btn btn-primary choose" disabled="disabled">
					<fmt:message key="lookup.action.choose"/>
				</button>
			</div>
		</div>
	</div>
	<input type="hidden" name="tags" value="price"/>
</form>
<form class="modal modal-lg fade dynamic lookup-modal sn-loc-lookup-modal weather-lookup-modal day-lookup-modal"
		action="<setup:url value='/a/location'/>" method="get">
	<div class="modal-dialog modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='lookup.weather.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='lookup.weather.intro'/></p>
				<div class="input-group my-3">
					<input type="text" class="form-control" maxlength="64" name="query" placeholder="<fmt:message key='lookup.weather.search.placeholder'/>"/>
					<button type="submit" class="btn btn-primary" data-loading-text="<fmt:message key='lookup.searching.label'/>">
						<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
						<span role="status"><fmt:message key='lookup.action.search'/></span>
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
				<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></a>
				<button type="button" class="btn btn-primary choose" disabled="disabled">
					<fmt:message key="lookup.action.choose"/>
				</button>
			</div>
		</div>
	</div>
	<input type="hidden" name="tags" value="weather"/>
</form>
<form class="modal modal-lg fade dynamic lookup-modal sn-loc-lookup-modal co2-lookup-modal"
		action="<setup:url value='/a/location'/>" method="get">
	<div class="modal-dialog modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key='lookup.co2.title'/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='lookup.co2.intro'/></p>
				<div class="input-group my-3">
					<input type="text" class="form-control" maxlength="64" name="query" placeholder="<fmt:message key='lookup.co2.search.placeholder'/>"/>
					<button type="submit" class="btn btn-primary" data-loading-text="<fmt:message key='lookup.searching.label'/>">
						<span class="spinner spinner-border spinner-border-sm hidden" aria-hidden="true"></span>
						<span role="status"><fmt:message key='lookup.action.search'/></span>
					</button>
				</div>

				<table class="table table-striped table-hover hidden search-results">
					<thead>
						<tr>
							<th><fmt:message key='lookup.co2.sourceName'/></th>
							<th><fmt:message key='lookup.co2.locationName'/></th>
							<th><fmt:message key='lookup.co2.country'/></th>
							<th><fmt:message key='lookup.co2.region'/></th>
						</tr>
						<tr class="template">
							<td data-tprop="sourceId"></td>
							<td data-tprop="m.name"></td>
							<td data-tprop="location.country"></td>
							<td data-tprop="location.region"></td>
						</tr>
					</thead>
					<tbody>
					</tbody>
				</table>

				<label id="co2-lookup-selected-label" class="hidden">
					<fmt:message key='lookup.selected.label'/>
					<span id="co2-lookup-selected-container"></span>
				</label>
			</div>
			<div class="modal-footer">
				<a href="#" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></a>
				<button type="button" class="btn btn-primary choose" disabled="disabled">
					<fmt:message key="lookup.action.choose"/>
				</button>
			</div>
		</div>
	</div>
	<input type="hidden" name="tags" value="co2"/>
</form>
<form id="add-component-instance-modal" class="modal fade dynamic" data-bs-backdrop="static" data-bs-keyboard="false" action="<setup:url value='/a/settings/manage/add'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title">
					<fmt:message key='settings.factory.add'>
						<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
					</fmt:message>
				</h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key='settings.factory.add.intro'/></p>
				<div class="col-12">
					<input type="text" class="form-control" maxlength="32" name="name" id="add-component-instance-name"
						placeholder="<fmt:message key='settings.factory.add.placeholder'/>"/>
				</div>
			</div>
			<div class="modal-footer">
				<sec:csrfInput/>
				<input type="hidden" name="uid" value="${factory.factoryUid}"/>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="settings.factory.add.label"/></button>
			</div>
		</div>
	</div>
</form>
