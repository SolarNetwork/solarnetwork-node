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
	</body>
</html>
