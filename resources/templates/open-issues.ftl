[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
[#if success]
<b>Open issues in ${repoName}:</b>
<p>
  [#if issues?? && issues?size > 0]
  <table>
    <tr><th>Number</th><th>Title</th><th>Raised By</th><th>Assigned To</th></tr>
    [#list issues as issue]
    <tr><td><b><a href="${issue.url}">${issue.number}</a></b></td><td>${issue.title}</td><td><a href="${issue.user.url}">${issue.user.login}</a></td><td>[#if issue.assignee??]<a href="${issue.assignee.url}">${issue.assignee.login}</a>[#else] [/#if]</td></tr>
    [/#list]
  </table>
  [#else]
  No open issues in this repository. 🎉
  [/#if]
</p>
[#else]
  [#if repoName??]
<p>Repository <b>${repoName}</b> doesn't exist.</p>
  [#else]
<p>No repository name provided - correct usage is: <b>open-issues repo-name</b></p>
  [/#if]
<p>The list of repositories can be obtained using the <b>list-repos</b> command.</p>
[/#if]
</messageML>
