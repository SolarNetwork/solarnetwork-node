<%@ taglib prefix="pack" uri="http://packtag.sf.net" %>
<setup:url value='/' var="basePath"/>
<meta name="base-path" content="${fn:endsWith(basePath, '/') 
	? fn:substring(basePath, 0, fn:length(basePath) - 1) 
	: basePath}" />
<pack:style context="${basePath}">
	/css/bootstrap.css
	/css/bootstrap-responsive.css
	/css/ladda.css
	/css/solarnode.css
	/css/fonts.css
	/css/font-awesome.css
</pack:style>
<sec:authorize access="!hasRole('ROLE_USER')">
	<setup:resources type="text/css"/>
</sec:authorize>
<sec:authorize access="hasRole('ROLE_USER')">
	<setup:resources type="text/css" role='USER'/>
</sec:authorize>
<pack:script context="${basePath}"> 
	/js-lib/jquery-1.12.4.js
	/js-lib/bootstrap.js
	/js-lib/ladda.js
	/js-lib/moment.js
	/js-lib/jquery.form.js
	/js-lib/stomp.js
	/js/global.js
	/js/global-websocket.js
	/js/global-platform.js
	/js/backups.js
	/js/datum.js
	/js/certs.js
	/js/login.js
	/js/settings.js
	/js/new-node.js
	/js/plugins.js
</pack:script>
<sec:authorize access="!hasRole('ROLE_USER')">
	<setup:resources type="application/javascript"/>
</sec:authorize>
<sec:authorize access="hasRole('ROLE_USER')">
	<setup:resources type="application/javascript" role='USER'/>
</sec:authorize>
