<c:choose>
	<c:when test="${results != null}">
		<p class="ping">
			<strong>Overall: </strong>
			<span${results.allGood ? '' : ' class="fail"'}>
				<c:choose>
					<c:when test="${results.allGood}">ALL_GOOD</c:when>
					<c:otherwise>One or more tests failed.</c:otherwise>
				</c:choose>
			</span>
		</p>
		<p>
			<strong>Date: </strong>
			${results.date}
		</p>
		<table class="ping table">
			<thead>
				<tr>
					<th>Test</th>
					<th>Status</th>
					<th>Execution time</th>
					<th>Message</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${results.results}" var="result">
					<tr>
						<th>
							${result.value.pingTestName}<br />
							<div class="caption">${result.key}</div>
						</th>
						<td${result.value.success ? '' : ' class="fail"'}>
							<c:choose>
								<c:when test="${result.value.success}">
									PASS
								</c:when>
								<c:otherwise>
									FAIL
								</c:otherwise>
							</c:choose>
						</td>
						<td>
							${result.value.duration}
						</td>
						<td>
							${result.value.message}
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:when>
	<c:otherwise>
		<p class="fail">FAIL: no ping results available</p>
	</c:otherwise>
</c:choose>
