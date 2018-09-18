[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
<b>Help for the <a href="https://github.com/finos-fdx/bot-github-chatops">GitHub ChatOps bot</a></b>
<p>Commands are entered in a message by themselves, with the command name as the first word in the message (if a command
   appears later in a message, that message is ignored).  Command names are case-<i><b>in</b></i>sensitive, although arguments sent to
   the command (e.g. repository names) are case-sensitive.
  <table>
    <tr><th>Command</th><th>Description</th></tr>
  [#list commands as command]
    <tr><td><b>${command[0]}</b></td><td>${command[1]}</td></tr>
  [/#list]
  </table>
</p>
</messageML>
