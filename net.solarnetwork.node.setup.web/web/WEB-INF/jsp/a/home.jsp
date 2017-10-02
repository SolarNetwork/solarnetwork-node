<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<p class="lead">
	<fmt:message key='home.intro-loggedin'>
		<fmt:param><sec:authentication property="principal.username" /></fmt:param>
	</fmt:message>
</p>

<section id="external-node-links" class="hide">
	<h2><fmt:message key="home.external-links.title"/></h2>	
	<p><fmt:message key="home.external-links.intro"/></p>
	<dl class="dl-horizontal">
		<dt class="node-dashboard hide">
			<fmt:message key="home.external-links.dashboard.title"/>
		</dt>
		<dd class="node-dashboard hide">
			<fmt:message key="home.external-links.dashboard.info"/>
		</dd>
		<dt class="node-dataview hide">
			<fmt:message key="home.external-links.dataview.title"/>
		</dt>
		<dd class="node-dataview hide">
			<fmt:message key="home.external-links.dataview.info"/>
		</dd>
		<dt class="dev-apitool hide">
			<fmt:message key="home.external-links.apitool.title"/>
		</dt>
		<dd class="dev-apitool hide">
			<fmt:message key="home.external-links.apitool.info"/>
		</dd>
	</dl>
</section>

<section id="datum-activity-seenprops" class="hide">
	<h2><fmt:message key="datum.activity.seenprops.title"/></h2>	
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
					<dl class="dl-horizontal datum-props hide">
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

<section id="datum-activity" class="hide">
	<h2><fmt:message key="datum.activity.title"/></h2>	
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
					<i class="event-icon hide"></i>
					<span data-tprop="type"></span>
				</td>
				<td data-tprop="sourceId"></td>
				<td>
					<dl class="datum-props hide">
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
