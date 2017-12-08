[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
[#macro issueStateToTextStyle issueState]
  [#switch issueState]
    [#case "open"]tempo-text-color--red[#break]
    [#default]tempo-text-color--gray
  [/#switch]
[/#macro]

  <table>
    <thead><tr><th>Number</th><th>State</th><th>Title</th><th>Raised By</th><th>Assigned To</th></tr></thead>
    <tbody>
    [#list issues as issue]
      <tr><td><b><a href="${issue.html_url}">${issue.number}</a></b></td><td><b class="[@issueStateToTextStyle issueState=issue.state/]">${issue.state}</b></td><td>${issue.title}</td><td><a href="${issue.user.html_url}">${issue.user.login}</a></td><td>[#if issue.assignee??]<a href="${issue.assignee.html_url}">${issue.assignee.login}</a>[/#if]</td></tr>
    [/#list]
    </tbody>
  </table>
