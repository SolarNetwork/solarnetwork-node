<section class="intro">
	<h2><fmt:message key="metrics.title"/></h2>
	<p><fmt:message key="metrics.intro"/></p>
</section>
<section class="init">
	<div class="progress" role="progressbar">
		<div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%;"></div>
    </div>
</section>
<section id="metrics" class="hidden ready">
	<p class="none"><fmt:message key="metrics.intro.none"/></p>
	<table class="table table-condensed some hidden" id="metrics-list">
		<thead>
			<tr>
				<th><fmt:message key="metric.idx.label"/></th>
				<th><fmt:message key="metric.ts.label"/></th>
				<th><fmt:message key="metric.type.label"/></th>
				<th><fmt:message key="metric.name.label"/></th>
				<th><fmt:message key="metric.value.label"/></th>
			</tr>
			<tr class="template item">
				<td data-tprop="idx"></td>
				<td data-tprop="displayTs"></td>
				<td data-tprop="type"></td>
				<td data-tprop="name"></td>
				<td data-tprop="value"></td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>
