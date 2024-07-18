<section class="intro">
	<p class="lead"><fmt:message key="metrics.intro"/></p>
</section>
<section class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</section>

<section id="metrics-most-recent" class="hidden">
	<h2><fmt:message key="metrics.mostRecent.title"/></h2>
	<div class="container">
		<div class="row fw-bold border-bottom">
			<div class="col-4"><fmt:message key="metric.ts.label"/></div>
			<div class="col-5"><fmt:message key="metric.name.label"/></div>
			<div class="col-3"><fmt:message key="metric.value.label"/></div>
		</div>
		<div class="row border-bottom brief-showcase template item">
			<div class="col-4" data-tprop="displayTs"></div>
			<div class="col-5" data-tprop="name"></div>
			<div class="col-3" data-tprop="value"></div>
		</div>
		<div class="list-container">
		</div>
	</div>
</section>

<section id="metrics" class="hidden ready" data-i18n-page="<fmt:message key='metrics.list.nav.page.label'/>">
	<h2><fmt:message key="metrics.title"/></h2>

	<p class="none"><fmt:message key="metrics.intro.none"/></p>

	<div class="row justify-content-between some">
		<div class="col-md-9">
			<p><fmt:message key="metrics.list.intro"/></p>
		</div>
		<form class="col-md-3 text-right form-inline">
			<div class="input-group">			
				<input type="search" id="metrics-list-filter-name" class="form-control search-query" 
					placeholder="<fmt:message key='metrics.list.filter.name.placeholder'/>" value="">
				<button class="btn btn-outline-secondary" type="button" id="metrics-list-refresh"
					title="<fmt:message key='metrics.list.refresh.btn'/>"><i class="bi bi-arrow-clockwise"></i></button>
			</div>
		</form>
	</div>

	<div class="container" id="metrics-list">
		<div class="row fw-bold border-bottom some hidden">
			<div class="col-1 text-end"><fmt:message key="metric.idx.label"/></div>
			<div class="col-3"><fmt:message key="metric.ts.label"/></div>
			<div class="col-4"><fmt:message key="metric.name.label"/></div>
			<div class="col-3"><fmt:message key="metric.value.label"/></div>
		</div>
		<div class="row border-bottom template item">
			<div class="col-1 text-end"data-tprop="idx"></div>
			<div class="col-3" data-tprop="displayTs"></div>
			<div class="col-4" data-tprop="name"></div>
			<div class="col-3" data-tprop="value"></div>
		</div>
		<div class="list-container some hidden">
		</div>
	</div>
	
	<div class="container text-center mt-3">
		<div class="row justify-content-md-center">
			<div class="col-md-auto">
				<div class="btn-group some" role="group" aria-label="<fmt:message key='metrics.list.nav.label'/>">
					<button id="metrics-list-nav-prev" type="button" class="btn btn-primary" disabled
						title="<fmt:message key='metrics.list.nav.prev.label'/>"><i class="bi bi-chevron-left"></i></button>
					
					<div class="btn-group dropup" role="group">
						<button id="metrics-list-nav-menu" type="button" class="btn btn-primary dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false" disabled>
							<fmt:message key='metrics.list.nav.select.btn'/>
						</button>
						<ul id="metrics-list-nav-menu-page-container" class="dropdown-menu">
						</ul>
					</div>
			
					<button id="metrics-list-nav-next" type="button" class="btn btn-primary" disabled
						title="<fmt:message key='metrics.list.nav.next.label'/>"><i class="bi bi-chevron-right"></i></button>
				</div>
			</div>
		</div>
	</div>
</section>
