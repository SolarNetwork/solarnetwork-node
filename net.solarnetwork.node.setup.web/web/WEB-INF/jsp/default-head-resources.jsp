<%@ taglib prefix="pack" uri="http://packtag.sf.net" %>
<c:url value='/' var="basePath"/>
<meta name="base-path" content="${fn:endsWith(basePath, '/') 
	? fn:substring(basePath, 0, fn:length(basePath) - 1) 
	: basePath}" />
<pack:style>
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
<pack:script> 
	/js-lib/jquery-1.7.1.js
	/js-lib/bootstrap.js
	/js-lib/ladda.js
	/js-lib/moment.js
	/js-lib/jquery.form.js
	/js-lib/stomp.js
	/js/global.js
	/js/backups.js
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
