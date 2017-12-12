[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
[#if success]
✅ Comment added to <a href="https://github.com/${org}/${repoName}/issues/${issueId}">${repoName} issue number ${issueId}</a>.
[#else]
<p class="tempo-text-color--red"><b>${errorMessage}</b></p>
<p>Correct usage is <b>add-comment repo-name issue-number comment-text</b></p>
<p>Help can be obtained using the <b>help</b> command.</p>
[/#if]
</messageML>
