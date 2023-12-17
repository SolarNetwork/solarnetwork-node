<section class="intro">
	<h2>
		<fmt:message key="controls.manage.title">
			<fmt:param value="${controlId}"/>
		</fmt:message>
	</h2>
	<p>
		<fmt:message key="controls.manage.intro">
			<fmt:param>${controlId}</fmt:param>
		</fmt:message>
	</p>
	<a href="<setup:url value='/a/controls'/>" class="btn btn-secondary">
		<i class="fas fa-arrow-left"></i>
		<fmt:message key="back.label"/>
	</a>
</section>
<c:if test="${not empty info}">
<section id="status">
	<h2><fmt:message key="controls.manage.status.title"/></h2>
	<p>
		<fmt:message key="controls.manage.status.intro"/>
	</p>
	<table class="table">
		<tbody>
			<tr>
				<td><strong><fmt:message key="controls.info.type.label"/></strong></td>
				<td>
					${info.type}
				</td>
			</tr>
			<tr>
				<td><strong><fmt:message key="controls.info.value.label"/></strong></td>
				<td>
					${info.value}
				</td>
			</tr>
			<tr>
				<td><strong><fmt:message key="controls.info.modifiable.label"/></strong></td>
				<td>
					${!info.readonly}
				</td>
			</tr>
		</tbody>
	</table>
</section>
<c:if test="${!info.readonly}">
<section id="instruct">
	<h2><fmt:message key="controls.manage.SetControlParameter.title"/></h2>
	<p>
		<fmt:message key="controls.manage.SetControlParameter.intro"/>
	</p>
	<form action="<setup:url value='/a/controls/setControlParameter'/>" method="post">
		<fieldset class="row">
			<label class="col-sm-2 col-md-1 col-form-label" for="SetControlParameter-parameterValue">
				<fmt:message key="controls.manage.SetControlParameter.parameterValue"/>
			</label>
			<div class="col-6">
				<div class="input-group">
					<input class="form-control" type="text" name="parameterValue" id="SetControlParameter-parameterValue" 
					maxLength="255" value="" />
					<button type="submit" class="btn btn-primary" id="submit"><fmt:message key='controls.manage.SetControlParameter.submit'/></button>
				</div>
			</div>
		</fieldset>
		<input type="hidden" name="controlId" value="${controlId}"/>
		<sec:csrfInput/>
	</form>
</section>
</c:if><%-- !info.readonly --%>
</c:if><%-- not empty info --%>
