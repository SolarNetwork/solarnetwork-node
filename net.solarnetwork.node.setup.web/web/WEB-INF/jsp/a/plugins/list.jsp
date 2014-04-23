<section class="intro">
	<p><fmt:message key="plugins.intro"/></p>
	<fmt:message key="plugins.loading.message" var="msgLoading"/>
	<fmt:message key="plugins.refresh.button" var="msgRefresh"/>
	<button id="plugins-refresh" class="btn btn-primary ladda-button expand-right" 
		data-loading-text="${msgLoading}"
		data-msg-loading="${msgLoading}"
		data-msg-refresh="${msgRefresh}">
		${msgRefresh}
	</button>
</section>
<section id="plugins">
</section>
