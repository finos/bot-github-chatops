[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
[#if success]
<b>Open issues in ${repoName}:</b>
<p>
  [#if issues?? && issues?size > 0]
    [#include "issues-summary-table.ftl"]
  [#else]
  No open issues in this repository. 🎉
  [/#if]
</p>
[#else]
<p>${errorMessage}<br/>
   Correct usage is <b>list-open-issues repo-name</b></p>
<p>The list of repositories can be obtained using the <b>list-repos</b> command.</p>
[/#if]
</messageML>
