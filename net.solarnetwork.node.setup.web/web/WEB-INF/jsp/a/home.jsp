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
	<div class="container datum-activity-seenprops">
		<div class="row fw-bold border-bottom">
			<div class="col-4"><fmt:message key='datum.sourceId.label'/></div>
			<div class="col-3"><fmt:message key='datum.created.label'/></div>
			<div class="col-6"><fmt:message key='datum.properties.label'/></div>
		</div>
		<div class="row activity template brief-showcase border-bottom">
			<div class="col-4 py-2" data-tprop="sourceId"></div>
			<div class="col-3 py-2" data-tprop="date"></div>
			<div class="col-6 py-2">
				<dl class="row datum-props hidden">
					<dt class="col-sm-5 datum-prop template" data-tprop="propName"></dt>
					<dd class="col-sm-7 datum-prop template" data-tprop="propValue"></dd>
				</dl>
			</div>
		</div>
		<div class="activity-container"></div>
	</div>
</section>

<section id="datum-activity" class="hidden">
	<h2>
		<a id="datum-activity" href="#datum-activity"
			class="anchor" aria-hidden="true"><i class="fas fa-link" aria-hidden="true"></i></a>			
		<fmt:message key="datum.activity.title"/>
	</h2>	
	
	<p><fmt:message key="datum.activity.intro"/></p>
	
	<div class="container datum-activity">
		<div class="row fw-bold border-bottom">
			<div class="col-2"><fmt:message key='datum.created.label'/></div>
			<div class="col-2"><fmt:message key='datum.activity.eventAndType.label'/></div>
			<div class="col-3"><fmt:message key='datum.sourceId.label'/></div>
			<div class="col-5"><fmt:message key='datum.properties.label'/></div>
		</div>
		<div class="row activity template brief-showcase border-bottom">
			<div class="col-2 py-2" data-tprop="date"></div>
			<div class="col-2 py-2">
				<i class="event-icon hidden"></i>
				<span data-tprop="type"></span>
			</div>
			<div class="col-3 py-2" data-tprop="sourceId"></div>
			<div class="col-5 py-2">
				<dl class="datum-props hidden">
					<dt class="datum-prop template" data-tprop="propName"></dt>
					<dd class="datum-prop template" data-tprop="propValue"></dd>
				</dl>
			</div>
		</div>
		<div class="activity-container"></div>
	</div>
</section>
