[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
[#macro issueStateToTextStyle issueState]
  [#switch issueState]
    [#case "open"]tempo-text-color--red[#break]
    [#default]tempo-text-color--gray
  [/#switch]
[/#macro]

  <table>
    <thead><tr><th>Number</th><th>Type</th><th>State</th><th>Title</th><th>Raised By</th><th>Assigned To</th><th>Created</th><th>Updated</th><th>View Issue Details Command</th></tr></thead>
    <tbody>
    [#list issues as issue]
      <tr><td><b><a href="${issue.html_url}">${issue.number}</a></b></td><td>[#if issue.pull_request??]Pull request[#else]Issue[/#if]</td><td><b class="[@issueStateToTextStyle issueState=issue.state/]">${issue.state}</b></td><td>${issue.title}</td><td><a href="${issue.user.html_url}">${issue.user.login}</a></td><td>[#if issue.assignee??]<a href="${issue.assignee.html_url}">${issue.assignee.login}</a>[/#if]</td><td>${issue.created_at?datetime.iso?string["yyyy-MM-dd h:mm:ssa z"]}</td><td>${issue.updated_at?datetime.iso?string["yyyy-MM-dd h:mm:ssa z"]}</td><td>id ${repoName} ${issue.number}</td></tr>
    [/#list]
    </tbody>
  </table>
