[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
<b>Administrative commands</b>
<p>Administrative commands are only available to the configured administrators of the bot, and can appear anywhere in
   the text of a message.  The bot will not respond to administrative commands in multi-party or chat rooms unless it is
   explicitly @mentioned at the start of the message (@mentioning the bot is unnecessary in 1:1 chats between an
   administrator and the bot)
  <table>
    <tr><th>Command</th><th>Description</th></tr>
  [#list commands as command]
    <tr><td><b>${command[0]}</b></td><td>${command[1]}</td></tr>
  [/#list]
  </table>
</p>
</messageML>
