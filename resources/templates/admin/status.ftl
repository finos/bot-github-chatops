[#ftl]
<messageML>
<b>GitHub ChatOps bot status as at ${now}:</b>
<p>
  <table>
    <tr><td><b>Symphony pod</b></td><td>${podName} v${podVersion}</td></tr>
    <tr><td><b>Runtime</b></td><td>Clojure v${clojureVersion} on JVM v${javaVersion} (${javaArchitecture})</td></tr>
    <tr><td><b>Bot build</b></td><td><a href="${gitUrl}">git revision ${gitRevision}</a>, built ${buildDate}</td></tr>
    <tr><td><b>Bot uptime</b></td><td>${botUptime}</td></tr>
    <tr><td><b>Time since last configuration reload</b></td><td>${lastReloadTime}</td></tr>
    <tr><td><b>Memory allocated</b></td><td>${allocatedRam}</td></tr>
  </table>
</p>
</messageML>
