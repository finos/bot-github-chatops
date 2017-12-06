[#ftl]
<messageML>
<b>Issues in ${repoName}:</b>
<p>
  <table>
    <tr><th>Number</th><th>Title</th><th>Raised By</th><th>Assigned To</th><th>State</th></tr>
  [#list issues as issue]
    <tr><td><b><a href="${issue.url}">${issue.number}</a></b></td><td>${issue.title}</td><td><a href="${issue.user.url}">${issue.user.login}</a></td><td>[#if issue.assignee??]<a href="${issue.assignee.url}">${issue.assignee.login}</a>[#else]n/a[/#if]</td><td>${issue.state}</td></tr>
  [/#list]
  </table>
</p>
</messageML>
