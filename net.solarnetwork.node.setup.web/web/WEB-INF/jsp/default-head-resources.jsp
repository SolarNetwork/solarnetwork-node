<%@ taglib prefix="pack" uri="http://packtag.sf.net" %>
<c:url value='/' var="basePath"/>
<meta name="base-path" content="${fn:endsWith(basePath, '/') 
	? fn:substring(basePath, 0, fn:length(basePath) - 1) 
	: basePath}" />
<pack:style>
	/css/bootstrap.css
	/css/bootstrap-responsive.css
	/css/solarnode.css
</pack:style>
<pack:script> 
	/js-lib/jquery-1.7.1.js
	/js-lib/bootstrap.js
	/js-lib/spin.js
	/js-lib/jquery.spin.js
	/js-lib/jquery.form.js
	/js/global.js
	/js/certs.js
	/js/settings.js
	/js/plugins.js
</pack:script>
