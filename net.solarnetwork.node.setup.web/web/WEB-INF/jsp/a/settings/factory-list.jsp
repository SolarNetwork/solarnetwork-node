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
	<div class="row action-bar">
		<div class="span2">
			<a href="<setup:url value='${navloc == "filters-component" ? "/a/settings/filters" : "/a/settings"}'/>" class="btn">
				<i class="icon-arrow-left"></i>
				<fmt:message key="back.label"/>
			</a>
		</div>
		<div class="span10 text-right">
			<c:if test="${fn:length(providers) > 0}">
				<button type="button" class="btn btn-default" data-toggle="modal" data-target="#remove-all-component-instance-modal">
					<i class="icon-trash"></i>
					<fmt:message key='settings.factory.removeall.label'>
						<fmt:param>${fn:length(providers)}</fmt:param>
						<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
					</fmt:message>
				</button>
			</c:if>
			<button type="button" class="btn btn-primary" id="add" data-toggle="modal" data-target="#add-component-instance-modal">
				<i class="icon-plus icon-white"></i>
				<fmt:message key='settings.factory.add'>
					<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
				</fmt:message>
			</button>
		</div>
	</div>
</section>

<section id="settings" class="carousel slide" data-interval="0">
	<c:if test="${fn:length(providers) > 1}">
		<ol class="carousel-indicators numbered">
			<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
				<li data-target="#settings" data-slide-to="${instanceStatus.index}" data-instance-key="${instance.key}"
					class="${instanceStatus.index == 0 ? 'active' : ''}">${instance.key}</li>
			</c:forEach>
		</ol>
	</c:if>
	<form class="form-horizontal" action="<setup:url value='/a/settings/save'/>" method="post">
		<c:if test="${fn:length(providers) > 0}">
			<div class="form-actions top">
				<button type="button" class="btn btn-primary" id="submit"><fmt:message key='settings.save'/></button>
			</div>
		</c:if>
		<div class="carousel-inner">
			<c:forEach items="${providers}" var="instance" varStatus="instanceStatus">
				<c:set var="instance" value="${instance}" scope="request"/>
				<c:set var="provider" value="${instance.value}" scope="request"/>
				<c:set var="instanceId" value="${provider.factoryInstanceUID}" scope="request"/>
				<!--  ${provider.settingUid} -->

				<fieldset class="item ${instanceStatus.index == 0 ? 'active' : ''}">
					<legend>
						<a id="${instance.key}"
							class="anchor"
							href="#${instance.key}"
							aria-hidden="true"><i class="fa fa-link" aria-hidden="true"></i></a>
						<setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/>
						${' '}
						${instance.key}
					</legend>
					<div class="instance-content"
							data-target="<setup:url value='/a/settings/manage'/>"
							data-factory-uid="${factory.factoryUid}"
							data-instance-key="${instance.key}">
					    <div class="progress progress-striped active">
					      <div class="bar" style="width: 100%;"></div>
					    </div>
					</div>
					<div class="control-group">
						<div class="controls">
							<button type="button" class="btn btn-danger delete-factory-instance"
									data-target="<setup:url value='/a/settings/manage/delete'/>"
									data-factory-uid="${factory.factoryUid}"
									data-instance-key="${instance.key}"
									>
								<fmt:message key='settings.factory.delete'>
									<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
									<fmt:param value="${instance.key}"/>
								</fmt:message>
							</button>
							<button type="button" class="btn btn-primary reset-factory-instance"
									data-target="<setup:url value='/a/settings/manage/reset'/>"
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
	$('#submit').click(function() {
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
<div id="alert-delete" class="alert alert-danger alert-block hidden">
	<button type="button" class="close" data-dismiss="alert">&times;</button>
	<h4><fmt:message key="settings.factory.delete.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.delete.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="delete.label"/>
	</button>
</div>
<div id="alert-reset" class="alert alert-danger alert-block hidden">
	<button type="button" class="close" data-dismiss="alert">&times;</button>
	<h4><fmt:message key="settings.factory.reset.alert.title"/></h4>
	<p>
		<fmt:message key="settings.factory.reset.alert.msg"/>
	</p>
	<button type="button" class="btn btn-danger submit">
		<fmt:message key="reset.label"/>
	</button>
</div>
<form id="remove-all-component-instance-modal" class="modal dynamic hide fade"
		action="<setup:url value='/a/settings/manage/removeall'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3><fmt:message key='settings.factory.removeall.title'/></h3>
	</div>
	<div class="modal-body">
		<p>
			<fmt:message key='settings.factory.removeall.intro'>
				<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
			</fmt:message>
		</p>
	</div>
	<div class="modal-footer">
		<button type="button" class="btn btn-default" data-dismiss="modal">
			<fmt:message key="cancel.label"/>
		</button>
		<button type="submit" class="btn btn-danger">
			<fmt:message key="settings.factory.removeall.remove.label"/>
		</button>
	</div>
	<sec:csrfInput/>
	<input type="hidden" name="uid" value="${factory.factoryUid}"/>
</form>
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
			<input type="text" class="span4" maxlength="64" name="query" placeholder="<fmt:message key='lookup.weather.search.placeholder'/>"/>
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
<form id="add-component-instance-modal" class="modal dynamic hide fade" data-backdrop="static" action="<setup:url value='/a/settings/manage/add'/>" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3>
			<fmt:message key='settings.factory.add'>
				<fmt:param><setup:message key="title" messageSource="${factory.messageSource}" text="${factory.displayName}"/></fmt:param>
			</fmt:message>
		</h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key='settings.factory.add.intro'/></p>
		<div class="form-inline">
			<input type="text" class="span5" maxlength="32" name="name" id="add-component-instance-name"
				placeholder="<fmt:message key='settings.factory.add.placeholder'/>"/>
		</div>
	</div>
	<div class="modal-footer">
		<sec:csrfInput/>
		<input type="hidden" name="uid" value="${factory.factoryUid}"/>
		<button type="button" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></button>
		<button type="submit" class="btn btn-primary"><fmt:message key="settings.factory.add.label"/></button>
	</div>
</form>
