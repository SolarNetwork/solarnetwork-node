<section class="intro">
	<h2><fmt:message key="cli-console.title"/></h2>
	<p><fmt:message key="cli-console.intro"/></p>
	<div class="alert alert-warning alert-dismissible">
		<fmt:message key="cli-console.warn"/>
		<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="<fmt:message key='close.label'/>"></button>
	</div>
</section>
<section id="cli-console-types">
	<p class="none"><fmt:message key="cli-console.types.intro.none"/></p>
	<div class="row template">
		<div class="col-md-11">
			<span data-tprop="name"></span>
		</div>
		<div class="col-md-1 text-right">
		    <button type="button" class="toggle btn btn-sm btn-secondary"
		    	data-label-on="<fmt:message key='settings.toggle.on'/>"
		    	data-label-off="<fmt:message key='settings.toggle.off'/>">
		    	<fmt:message key='settings.toggle.off'/>
		    </button>
		</div>
	</div>
	<div class="row">
		<div class="col-md-12 text-right">
			<button type="button" id="cli-console-clear-logging" class="btn btn-secondary" disabled
				title="<fmt:message key='cli-console.commands.clear.label'/>">
				<i class="bi bi-trash3"></i>
			</button>
			<button type="button" id="cli-console-logging-toggle" class="toggle btn btn-success"
				data-label-on="<fmt:message key='cli-console.commands.pause.label'/>"
				data-label-off="<fmt:message key='cli-console.commands.play.label'/>"
				data-class-on="bi-pause-circle"
				data-class-off="bi-play-circle"
				title="<fmt:message key='cli-console.commands.pause.label'/>">
				<i class="bi bi-pause-circle"></i>
			</button>
		</div>
	</div>
	<hr class="some">
	<div class="list-container"></div>
</section>
<hr>
<section id="cli-console">
	<div class="item template alert alert-light d-flex align-items-center brief-showcase-gray">
		<span class="cmd" data-tprop="command"></span>
		<div class="d-flex ms-auto align-items-center">
			<button type="button" class="btn btn-close" data-bs-dismiss="alert" aria-label="<fmt:message key='close.label'/>"></button>
			<button type="button" class="btn copy" tabindex="-1" aria-label="<fmt:message key='copy.label'/>"><i class="bi bi-clipboard2"></i></button>
		</div>
	</div>
	<div class="row">
		<div class="col-md-12 list-container"></div>
	</div>
</section>
