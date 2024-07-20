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
			<div class="col-3 text-end"><span class="number-whole"><fmt:message key="metric.value.label"/></span><span class="number-dot invisible">.</span><span class="number-fraction"></span></div>
		</div>
		<div class="row py-1 border-bottom brief-showcase template item">
			<div class="col-4" data-tprop="displayTs"></div>
			<div class="col-5" data-tprop="name"></div>
			<div class="col-3 text-end"><span class="number-whole" data-tprop="valueWhole"></span><span class="number-dot">.</span><span class="number-fraction" data-tprop="valueFraction"></span></div>
		</div>
		<div class="list-container">
		</div>
	</div>
</section>

<section id="metrics-aggregate" class="hidden some"
		data-i18n-min="<fmt:message key='metrics.aggregate.min.label'/>"
		data-i18n-max="<fmt:message key='metrics.aggregate.max.label'/>"
		data-i18n-avg="<fmt:message key='metrics.aggregate.avg.label'/>"
		data-i18n-q="<fmt:message key='metrics.aggregate.q.label'/>"
		>
	<h2><fmt:message key="metrics.aggregate.title"/></h2>
	<p><fmt:message key="metrics.aggregate.intro"/></p>
	
	<form class="row g-3" id="metrics-aggregate-filter-form">
		<div class="col-sm-2">
			<label for="metrics-aggregate-filter-from"><fmt:message key='metrics.aggregate.filter.from.label'/></label>
			<input type="date" name="start" class="form-control" id="metrics-aggregate-filter-from">
		</div>
		<div class="col-sm-2">
			<label for="metrics-aggregate-filter-to"><fmt:message key='metrics.aggregate.filter.to.label'/></label>
			<input type="date" name="end" class="form-control" id="metrics-aggregate-filter-to">
		</div>
		<div class="col-auto">
			<label for="metrics-aggregate-filter-from"><fmt:message key='metrics.aggregate.filter.stats.label'/></label>
			<div class="row mt-2">
				<div class="col-auto">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" name="aggs" value="min" id="metrics-aggregate-filter-agg-min" checked>
						<label class="form-check-label" for="metrics-aggregate-filter-agg-min">
							<fmt:message key='metrics.aggregate.min.label'/>
						</label>
					</div>
				</div>
				<div class="col-auto">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" name="aggs" value="max" id="metrics-aggregate-filter-agg-max" checked>
						<label class="form-check-label" for="metrics-aggregate-filter-agg-max">
							<fmt:message key='metrics.aggregate.max.label'/>
						</label>
					</div>
				</div>
				<div class="col-auto">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" name="aggs" value="avg" id="metrics-aggregate-filter-agg-avg" checked>
						<label class="form-check-label" for="metrics-aggregate-filter-agg-avg">
							<fmt:message key='metrics.aggregate.avg.label'/>
						</label>
					</div>
				</div>
			</div>
		</div>
		<div class="col-sm-2">
			<label for="metrics-aggregate-filter-agg-p1-inc"><fmt:message key='metrics.aggregate.p1.label'/>: <span id="metrics-aggregate-filter-agg-p1-disp">25</span>%</label>
			<div class="row mt-2">
				<div class="col-auto pe-0">
					<input class="form-check-input" type="checkbox" id="metrics-aggregate-filter-agg-p1-inc" checked>
				</div>
				<div class="col">
					<input type="range" min="1" max="99" value="25" name="p1" class="form-range" id="metrics-aggregate-filter-agg-p1">
				</div>
			</div>
		</div>
		<div class="col-sm-2">
			<label for="metrics-aggregate-filter-agg-p2-inc"><fmt:message key='metrics.aggregate.p2.label'/>: <span id="metrics-aggregate-filter-agg-p2-disp">75</span>%</label>
			<div class="row mt-2">
				<div class="col-auto pe-0">
					<input class="form-check-input" type="checkbox" id="metrics-aggregate-filter-agg-p2-inc" checked>
				</div>
				<div class="col">
					<input type="range" min="1" max="99" value="75" name="p1" class="form-range" id="metrics-aggregate-filter-agg-p2">
				</div>
			</div>
		</div>
	</form>

	<div class="container mt-3">
		<div class="row fw-bold border-bottom">
			<div class="col-5"><fmt:message key="metric.name.label"/></div>
			<div class="col-4"><fmt:message key="metric.type.label"/></div>
			<div class="col-3 text-end"><span class="number-whole"><fmt:message key="metric.value.label"/></span><span class="number-dot invisible">.</span><span class="number-fraction"></span></div>
		</div>
		<div class="row py-1 border-bottom template item">
			<div class="col-5" data-tprop="name"></div>
			<div class="col-4" data-tprop="displayType"></div>
			<div class="col-3 text-end"><span class="number-whole" data-tprop="valueWhole"></span><span class="number-dot">.</span><span class="number-fraction" data-tprop="valueFraction"></span></div>
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
			<div class="col-3 text-end"><span class="number-whole"><fmt:message key="metric.value.label"/></span><span class="number-dot invisible">.</span><span class="number-fraction"></span></div>
		</div>
		<div class="row py-1 border-bottom template item">
			<div class="col-1 text-end"data-tprop="idx"></div>
			<div class="col-3" data-tprop="displayTs"></div>
			<div class="col-4" data-tprop="name"></div>
			<div class="col-3 text-end"><span class="number-whole" data-tprop="valueWhole"></span><span class="number-dot">.</span><span class="number-fraction" data-tprop="valueFraction"></span></div>
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
