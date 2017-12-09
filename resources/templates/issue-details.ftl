[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
[#if success]
<br/>
<p>
  [#if issues?? && issues?size > 0]
    [#list issues as issue]
      [#include "issue-detail.ftl"]
    [/#list]
  [#else]
  No issues requested.
  [/#if]
</p>
[#else]
<p class="tempo-text-color--red"><b>${errorMessage}</b></p>
<p>Correct usage is <b>issue-details repo-name issue-number [issue-number issue-number ...]</b></p>
<p>The list of repositories can be obtained using the <b>list-repos</b> command.<br/>
   The list of open issues in a repository can be obtained using the <b>list-open-issues</b> command.</p>
[/#if]
</messageML>
