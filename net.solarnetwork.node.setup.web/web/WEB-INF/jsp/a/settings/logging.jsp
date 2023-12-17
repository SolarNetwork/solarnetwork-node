<section class="intro">
	<h2>
		<fmt:message key="logging.title"/>
	</h2>
	<p><fmt:message key="logging.intro"/></p>
	<div class="d-flex justify-content-end">
		<button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#edit-logger-level-modal">
			<i class="bi bi-plus-lg"></i>
			<span><fmt:message key="logging.level.add"/></span>
		</button>
	</div>
</section>
<section id="logging">
	<h2><fmt:message key="logging.levels.title"/></h2>
	<table class="table table-condensed table-hover" id="logging-levels">
		<thead>
			<tr>
				<th><fmt:message key="logging.levels.logger.label"/></th>
				<th><fmt:message key="logging.levels.level.label"/></th>
			</tr>
			<tr class="template clickable item">
				<td><a href="#" data-tprop="logger"></a></td>
				<td data-tprop="level"></td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<form id="edit-logger-level-modal" class="modal modal-lg fade dynamic" data-bs-backdrop="static" data-bs-keyboard="false" action="<setup:url value='/a/logging/levels'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h3 class="modal-title"><fmt:message key="logging.levels.edit.title"/></h3>
				<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<fmt:message key='close.label'/>"></button>
			</div>
			<div class="modal-body">
				<p><fmt:message key="logging.levels.edit.intro"/></p>
				<label class="form-label" for="logging-logger-levels-logger"><fmt:message key="logging.levels.logger.label"/></label>
				<input class="form-control" type="text" maxlength="256" name="logger" id="logging-logger-levels-logger" aria-describedby="logging-logger-levels-logger-help">
				<div class="form-text" id="logging-logger-levels-logger-help"><fmt:message key="logging.levels.logger.caption"/></div>

				<label class="form-label mt-3" for="logging-logger-levels-level"><fmt:message key="logging.levels.level.label"/></label>
				<select class="form-select" name="level" id="logging-logger-levels-level" aria-describedby="logging-logger-levels-level-help">
					<option value="trace"><fmt:message key="logging.levels.TRACE.label"/></option>
					<option value="debug"><fmt:message key="logging.levels.DEBUG.label"/></option>
					<option value="info" selected><fmt:message key="logging.levels.INFO.label"/></option>
					<option value="warn"><fmt:message key="logging.levels.WARN.label"/></option>
					<option value="error"><fmt:message key="logging.levels.ERROR.label"/></option>
					<option value="off"><fmt:message key="logging.levels.OFF.label"/></option>
					<option value="inherit"><fmt:message key="logging.levels.INHERIT.label"/></option>
				</select>
				<span class="form-text" id="logging-logger-levels-level-help"><fmt:message key="logging.levels.level.caption"/></span>

				<p class="create mt-3"><fmt:message key="logging.levels.edit.loggers.intro"/></p>
				<select class="create form-select" name="loggers" readonly size="10" id="logging-loggers">
					<option value="trace"><fmt:message key="logging.levels.TRACE.label"/></option>
					<option value="debug"><fmt:message key="logging.levels.DEBUG.label"/></option>
					<option value="info" selected><fmt:message key="logging.levels.INFO.label"/></option>
					<option value="warn"><fmt:message key="logging.levels.WARN.label"/></option>
					<option value="error"><fmt:message key="logging.levels.ERROR.label"/></option>
					<option value="off"><fmt:message key="logging.levels.OFF.label"/></option>
				</select>
			</div>
			<div class="modal-footer">
				<sec:csrfInput/>
				<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><fmt:message key='close.label'/></button>
				<button type="submit" class="btn btn-primary"><fmt:message key="logging.levels.edit.save.label"/></button>
			</div>
		</div>
	</div>
</form>
