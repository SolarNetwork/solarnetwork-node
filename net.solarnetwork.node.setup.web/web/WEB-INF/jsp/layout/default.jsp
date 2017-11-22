<!DOCTYPE html>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<html lang="en">
	<tiles:useAttribute name="navloc" scope="request"/>
	<tiles:insertAttribute name="head" />
	<body>
		<c:import url="/WEB-INF/jsp/navbar.jsp"/>
		<tiles:insertAttribute name="header" />
		<div class="container" id="body-container">			
			<c:if test="${not empty statusMessageKey}">
				<div class="alert alert-success">
					<c:choose>
						<c:when test="${fn:startsWith(statusMessageKey, 'registration.')}">
							<fmt:message key="${statusMessageKey}">
								<fmt:param><sec:authentication property="principal.username" /></fmt:param>
							</fmt:message>
						</c:when>
						<c:otherwise>
							<fmt:message key="${statusMessageKey}">
								<c:if test="${not empty statusMessageParam0}">
									<fmt:param value="${statusMessageParam0}"/>
								</c:if>
							</fmt:message>
						</c:otherwise>
					</c:choose>
				</div>
				<c:remove var="statusMessageKey" scope="session"/>
				<c:remove var="statusMessageParam0" scope="session"/>
			</c:if>
			<c:if test="${not empty errorMessageKey}">
				<div class="alert alert-error">
					<fmt:message key="${errorMessageKey}">
						<c:if test="${not empty errorMessageKey}">
							<fmt:param value="${errorMessageParam0}"/>
						</c:if>
					</fmt:message>
				</div>
				<c:remove var="errorMessageKey" scope="session"/>
				<c:remove var="errorMessageParam0" scope="session"/>
			</c:if>
			<tiles:insertAttribute name="body" />
		</div>
		
		<%-- System lock overlay --%>
		<setup:url value="/pub/platform/state" var="urlPlatformState"/>
		<form id="platform-lock-modal" class="modal dynamic hide fade" action="${urlPlatformState}" method="get"
				data-backdrop="static" data-keyboard="false">
			<div class="modal-header">
				<h3 class="info-title"><fmt:message key="platform.lock.title"/></h3>
			</div>
			<div class="modal-body">
				<p class="info-message hide-while-restarting"></p>
				<div class="restart-required hide-while-restarting hide alert">
					<fmt:message key='platform.lock.restartRequired.warning'/>
				</div>
				<div class="restarting hide alert alert-info">
					<fmt:message key="platform.lock.taskComplete.msg"/><span> </span><fmt:message key="restart.underway"/>
				</div>
				<div class="progress progress-striped active">
					<div class="bar"></div>
			    </div>
			</div>
		</form>
		
		<%-- Application scoped setup resource integration support  --%>
		<setup:resources role="USER" type="text/html" inline="true" scope="Application"/>
	</body>
</html>
