[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
[#if success]
<b>${summary?capitalize} issues in ${repoName}:</b>
<p>
  [#if issues?? && issues?size > 0]
    [#include "issues-summary-table.ftl"]
  [#else]
  No matching issues found.
  [/#if]
</p>
[#else]
<p class="tempo-text-color--red"><b>${errorMessage}</b></p>
<p>Correct usage is <b>${commandName} repo-name</b></p>
<p>The list of repositories can be obtained using the <b>list-repos</b> command.</p>
[/#if]
</messageML>
