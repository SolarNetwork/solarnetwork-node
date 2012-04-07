<!DOCTYPE html>
<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<html xmlns="http://www.w3.org/1999/xhtml">
	<tiles:insertAttribute name="head" />
	<body>
		<div class="content">
			<tiles:insertAttribute name="header" />
			<tiles:insertAttribute name="body" />
		</div>
	</body>
</html>
