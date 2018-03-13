[#ftl output_format="HTML" auto_esc=true strip_whitespace=true]
<messageML>
<b>GitHub ChatOps bot status as at ${now}:</b>
<p>
  <table>
    <tr><td><b>Symphony</b></td><td>${podName} (pod v${podVersion}, agent v${agentVersion})</td></tr>
    <tr><td><b>Runtime</b></td><td>Clojure v${clojureVersion} on JVM v${javaVersion} (${javaArchitecture})</td></tr>
    <tr><td><b>Bot build</b></td><td><a href="${gitUrl}">git revision ${gitRevision}</a>, built ${buildDate}</td></tr>
    <tr><td><b>Bot uptime</b></td><td>${botUptime}</td></tr>
    <tr><td><b>Time since last configuration reload</b></td><td>${lastReloadTime}</td></tr>
    <tr><td><b>Memory</b></td><td>${usedRam} used of ${allocatedRam} allocated (${percentRamUsed?round?c}%)</td></tr>
  </table>
</p>
</messageML>
