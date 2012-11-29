<div class="intro"><fmt:message key="node.setup.success.intro"/></div>

<div>
	
	<table>
		<tr><td><fmt:message key="node.setup.success.service"/></td><td>${details.hostName}</td></tr>
	
		<tr><td><fmt:message key="node.setup.success.user"/></td><td>${details.userName}</td></tr>
	
		<tr><td><fmt:message key="node.setup.success.nodeId"/></td><td>${details.nodeId}</td></tr>
	</table>
</div>

<div><fmt:message key="node.setup.success.visit"><fmt:param value="http://${details.hostName}:${details.hostPort}/solarreg/u/my-nodes"/></fmt:message></div>