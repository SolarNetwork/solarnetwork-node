<%@ taglib prefix="pack" uri="http://packtag.sf.net" %>
<setup:url value='/' var="basePath"/>
<meta name="base-path" content="${fn:endsWith(basePath, '/')
	? fn:substring(basePath, 0, fn:length(basePath) - 1)
	: basePath}">
<link rel="icon" type="image/png" href="<setup:url value='/img/favicon.png'/>">
<pack:style context="${basePath}">
	/css/bootstrap.css
	/css/solarnode.css
	/css/fonts.css
	/css/bootstrap-icons.css
</pack:style>
<sec:authorize access="!hasRole('ROLE_USER')">
	<setup:resources type="text/css"/>
</sec:authorize>
<sec:authorize access="hasRole('ROLE_USER')">
	<setup:resources type="text/css" role='USER'/>
</sec:authorize>
<pack:script context="${basePath}">
	/js-lib/jquery-3.7.1.js
	/js-lib/bootstrap.bundle.js
	/js-lib/moment.js
	/js-lib/jquery.form.js
	/js-lib/stomp.js
	/js-lib/d3v4.js
	/js/global.js
	/js/global-websocket.js
	/js/global-platform.js
	/js/backups.js
	/js/datum.js
	/js/datum-sources.js
	/js/certs.js
	/js/hosts.js
	/js/login.js
	/js/metrics.js
	/js/settings.js
	/js/new-node.js
	/js/packages.js
	/js/plugins.js
	/js/datum-charts.js
	/js/logging.js
	/js/opmodes.js
	/js/cli-console.js
	/js/sectoks.js
	/js/locstate.js
</pack:script>
<sec:authorize access="!hasRole('ROLE_USER')">
	<setup:resources type="application/javascript"/>
</sec:authorize>
<sec:authorize access="hasRole('ROLE_USER')">
	<setup:resources type="application/javascript" role='USER'/>
</sec:authorize>
