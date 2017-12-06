[#ftl]
<messageML>
<b>Repos:</b>
<p>
  <table>
  [#list repos as repo]
    <tr><td><a href="${repo[1]}">${repo[0]}</a></td></tr>
  [/#list]
  </table>
</p>
</messageML>
