<section class="intro">
	<h2><fmt:message key="metrics.title"/></h2>
	<p><fmt:message key="metrics.intro"/></p>
</section>
<section class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</section>
<section id="metrics" class="hidden ready" data-i18n-page="<fmt:message key='metrics.list.nav.page.label'/>">
	<p class="none"><fmt:message key="metrics.intro.none"/></p>
	<div class="row justify-content-between some">
		<div class="col-md-9">
			<p><fmt:message key="metrics.list.intro"/></p>
		</div>
		<form class="col-md-3 text-right form-inline">
			<input type="search" id="metrics-list-filter-name" class="form-control search-query" 
				placeholder="<fmt:message key='metrics.list.filter.name.placeholder'/>" value="">
		</form>
	</div>
	<table class="table table-condensed some hidden" id="metrics-list">
		<thead>
			<tr>
				<th><fmt:message key="metric.idx.label"/></th>
				<th><fmt:message key="metric.ts.label"/></th>
				<th><fmt:message key="metric.name.label"/></th>
				<th><fmt:message key="metric.value.label"/></th>
			</tr>
			<tr class="template item">
				<td data-tprop="idx"></td>
				<td data-tprop="displayTs"></td>
				<td data-tprop="name"></td>
				<td data-tprop="value"></td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
	
	<div class="container text-center">
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
