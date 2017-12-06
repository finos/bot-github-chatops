;
; Copyright © 2017 Symphony Software Foundation
; SPDX-License-Identifier: Apache-2.0
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns bot-github-chatops.commands
  (:require [clojure.string                :as s]
            [clojure.pprint                :as pp]
            [clojure.tools.logging         :as log]
            [clojure.java.io               :as io]
            [mount.core                    :as mnt :refer [defstate]]
            [clj-time.core                 :as tm]
            [clj-time.format               :as tf]
            [clj-symphony.user             :as syu]
            [clj-symphony.message          :as sym]
            [clj-symphony.stream           :as sys]
            [bot-github-chatops.utils      :as u]
            [bot-github-chatops.config     :as cfg]
            [bot-github-chatops.connection :as cnxn]
            [bot-github-chatops.template   :as tem]
            [bot-github-chatops.github     :as gh]))


(defn- list-repos!
  "Lists the GitHub repos the bot is able to interact with."
  [stream-id _]
  (let [repos (gh/repos)]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       (tem/render "list-repos.ftl"
                                   { :repos (map #(vector % (gh/repo-url %)) repos) }))))

(defn- list-issues!
  "Lists the issues for the given repository (which must be supplied immediately after the command name)."
  [stream-id plain-text]
  (let [repo-name (second (s/split plain-text #"\s+"))
        issues    (gh/issues repo-name)]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       (tem/render "list-issues.ftl"
                                   { :repoName repo-name
                                     :issues   issues } ))))

(declare help!)

; Table of commands - each of these must be a function of 2 args (strean-id, plain-text-of-message)
(def ^:private commands
  {
    "list-repos"  #'list-repos!
    "list-issues" #'list-issues!
    "help"        #'help!
  })

(defn- help!
  "Displays this help message."
  [stream-id _]
  (sym/send-message! cnxn/symphony-connection
                     stream-id
                     (tem/render "help.ftl"
                                 { :commands (map #(vector (key %) (:doc (meta (val %)))) (sort-by key commands)) })))

(defn- process-command!
  "Looks for given command in the message text, executing it and returning true if it was found, false otherwise."
  [from-user-id stream-id text plain-text command]
  (if (s/starts-with? plain-text command)
    (do
      (log/debug "Command"      command
                 "requested by" (:email-address (syu/user cnxn/symphony-connection from-user-id))
                 "in stream"    stream-id)
      ((get commands command) stream-id plain-text)
      true)
    false))

(defn process-commands!
  "Process any commands in the given message.  Returns true if a command (or help) was displayed, false otherwise."
  [from-user-id stream-id text entity-data]
  (if (not (s/blank? text))
    (boolean (some identity (doall (map (partial process-command! from-user-id stream-id text (sym/to-plain-text text)) (keys commands)))))
    false))
