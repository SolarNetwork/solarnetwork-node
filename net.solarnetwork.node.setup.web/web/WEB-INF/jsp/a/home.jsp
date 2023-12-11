<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<p class="lead">
	<fmt:message key='home.intro-loggedin'>
		<fmt:param><sec:authentication property="principal.username" /></fmt:param>
	</fmt:message>
</p>

<section id="external-node-links" class="hidden">
	<h2>
		<a id="links" href="#links"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="home.external-links.title"/>
	</h2>	
	<p><fmt:message key="home.external-links.intro"/></p>
	<dl class="row">
		<dt class="col-sm-3 node-dashboard hidden">
			<fmt:message key="home.external-links.dashboard.title"/>
		</dt>
		<dd class="col-sm-9 node-dashboard hidden">
			<fmt:message key="home.external-links.dashboard.info"/>
		</dd>
		<dt class="col-sm-3 node-dataview hidden">
			<fmt:message key="home.external-links.dataview.title"/>
		</dt>
		<dd class="col-sm-9 node-dataview hidden">
			<fmt:message key="home.external-links.dataview.info"/>
		</dd>
		<dt class="col-sm-3 dev-apitool hidden">
			<fmt:message key="home.external-links.apitool.title"/>
		</dt>
		<dd class="col-sm-9 dev-apitool hidden">
			<fmt:message key="home.external-links.apitool.info"/>
		</dd>
	</dl>
</section>

<section id="datum-activity-charts" class="hidden">
	<h2>
		<a id="charts" href="#charts"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="datum.charts.title"/>
	</h2>
	<div id="datum-activity-charts-container"></div>
	<div class="clearfix"></div>
</section>

<section id="datum-activity-seenprops" class="hidden">
	<h2>
		<a id="datum-props" href="#datum-props"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="datum.activity.seenprops.title"/>
	</h2>	
	<p><fmt:message key="datum.activity.seenprops.intro"/></p>
	<table class="table datum-activity-seenprops">
		<thead>
			<tr>
				<th><fmt:message key='datum.sourceId.label'/></th>
				<th><fmt:message key='datum.created.label'/></th>
				<th><fmt:message key='datum.properties.label'/></th>
			</tr>
			<tr class="template brief-showcase">
				<td data-tprop="sourceId"></td>
				<td data-tprop="date"></td>
				<td>
					<dl class="row datum-props hidden">
						<dt class="col-sm-3 datum-prop template" data-tprop="propName"></dt>
						<dd class="col-sm-9 datum-prop template" data-tprop="propValue"></dd>
					</dl>
				</td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>

<section id="datum-activity" class="hidden">
	<h2>
		<a id="datum-activity" href="#datum-activity"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="datum.activity.title"/>
	</h2>	
	<p><fmt:message key="datum.activity.intro"/></p>
	<table class="table datum-activity">
		<thead>
			<tr>
				<th><fmt:message key='datum.created.label'/></th>
				<th><fmt:message key='datum.activity.eventAndType.label'/></th>
				<th><fmt:message key='datum.sourceId.label'/></th>
				<th><fmt:message key='datum.properties.label'/></th>
			</tr>
			<tr class="template brief-showcase">
				<td data-tprop="date"></td>
				<td>
					<i class="event-icon hidden"></i>
					<span data-tprop="type"></span>
				</td>
				<td data-tprop="sourceId"></td>
				<td>
					<dl class="datum-props hidden">
						<dt class="datum-prop template" data-tprop="propName"></dt>
						<dd class="datum-prop template" data-tprop="propValue"></dd>
					</dl>
				</td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>
