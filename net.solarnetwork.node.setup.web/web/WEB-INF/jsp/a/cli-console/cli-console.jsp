<section class="intro">
	<h2><fmt:message key="cli-console.title"/></h2>
	<p><fmt:message key="cli-console.intro"/></p>
	<div class="alert alert-warning">
		<button type="button" class="btn-close" data-bs-dismiss="alert alert-warning" aria-label="<fmt:message key='close.label'/>"></button>
		<fmt:message key="cli-console.warn"/>
	</div>
</section>
<section id="cli-console-types">
	<p class="none"><fmt:message key="cli-console.types.intro.none"/></p>
	<div class="row template">
		<div class="col-md-11">
			<span data-tprop="name"></span>
		</div>
		<div class="col-md-1 text-right">
		    <button type="button" class="toggle btn btn-sm"
		    	data-label-on="<fmt:message key='settings.toggle.on'/>"
		    	data-label-off="<fmt:message key='settings.toggle.off'/>">
		    	<fmt:message key='settings.toggle.off'/>
		    </button>
		</div>
	</div>
	<div class="row">
		<div class="col-md-12 text-right">
			<button type="button" id="cli-console-clear-logging" class="btn btn-default" disabled
				title="<fmt:message key='cli-console.commands.clear.label'/>">
				<i class="far fa-trash-can"></i>
			</button>
			<button type="button" id="cli-console-logging-toggle" class="toggle btn btn-success"
				data-label-on="<fmt:message key='cli-console.commands.pause.label'/>"
				data-label-off="<fmt:message key='cli-console.commands.play.label'/>"
				data-class-on="fa-circle-pause"
				data-class-off="fa-circle-play"
				title="<fmt:message key='cli-console.commands.pause.label'/>">
				<i class="far fa-circle-pause"></i>
			</button>
		</div>
	</div>
	<hr class="some">
	<div class="list-container"></div>
</section>
<hr>
<section id="cli-console">
	<div class="item template well well-small brief-showcase-gray">
		<button type="button" class="btn-close" data-bs-dismiss="alert alert-warning" aria-label="<fmt:message key='close.label'/>"></button>
		<button type="button" class="copy"><i class="far fa-clipboard"></i></button>
		<span class="cmd" data-tprop="command"></span>
	</div>
	<div class="row">
		<div class="col-md-12 list-container"></div>
	</div>
</section>
