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


(def iso-date-formatter (:date-time tf/formatters))

(defn- list-repos!
  "Lists the GitHub repos the bot is able to interact with."
  [stream-id _ _]
  (let [message (tem/render "list-repos.ftl"
                            { :repos (gh/repos) })]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))
(def ^:private list-repos-short! "Shorthand for list-repos - see help for that command for details." list-repos!)


(defn- list-issues!
  "Lists issues for the given repository in the given state."
  [stream-id words summary command-name issue-filter]
  (let [arguments                      (rest words)
        repo-name                      (first arguments)
        [success error-message]        (if-not (s/blank? repo-name)
                                         [true  nil]
                                         [false "No repository was provided, but it is required."])
        [success error-message issues] (if success
                                         (try
                                           [true nil (gh/issues repo-name issue-filter)]
                                           (catch Exception e
                                             [false (str "Invalid repository '" repo-name "'.") nil]))
                                         [success error-message nil])
        message                        (tem/render "list-issues.ftl"
                                                   { :success      success
                                                     :repoName     repo-name
                                                     :summary      summary
                                                     :commandName  command-name
                                                     :issues       issues
                                                     :errorMessage error-message } )]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))


(defn- list-all-issues!
  "Lists all issues in the given repository, which must be supplied immediately after the command e.g. list-all-issues MyRepository"
  [stream-id _ words]
  (list-issues! stream-id words "all" "list-all-issues" { :state "all" }))
(def ^:private list-all-issues-short! "Shorthand for list-all-issues - see help for that command for details." list-all-issues!)


(defn- list-open-issues!
  "Lists open issues in the given repository, which must be supplied immediately after the command e.g. list-open-issues MyRepository"
  [stream-id _ words]
  (list-issues! stream-id words "open" "list-open-issues" { :state "open" }))
(def ^:private list-open-issues-short! "Shorthand for list-open-issues - see help for that command for details." list-open-issues!)


(defn- list-closed-issues!
  "Lists closed issues in the given repository, which must be supplied immediately after the command e.g. list-closed-issues MyRepository"
  [stream-id _ words]
  (list-issues! stream-id words "closed" "list-closed-issues" { :state "closed" }))
(def ^:private list-closed-issues-short! "Shorthand for list-closed-issues - see help for that command for details." list-closed-issues!)


(defn- list-recently-updated-issues!
  "Lists issues updated in the last month in the given repository, which must be supplied immediately after the command e.g. list-recently-updated-issues MyRepository"
  [stream-id _ words]
  (list-issues! stream-id words "recently updated" "list-recently-updated-issues" { :state "all" :since (tf/unparse iso-date-formatter (tm/minus (tm/now) (tm/months 1))) }))
(def ^:private list-recently-updated-issues-short! "Shorthand for list-recently-updated-issues - see help for that command for details." list-recently-updated-issues!)


(defn- issue-details!
  "Displays details on one or more issues in a given repository. The repository name must be supplied immediately after the command, followed by one or more issue ids e.g. issue-details MyRepository 14 17 22"
  [stream-id _ words]
  (let [arguments                         (rest words)
        repo-name                         (first arguments)
        [success error-message]           (if-not (s/blank? repo-name)
                                            [true  nil]
                                            [false "No repository was provided, but it is required."])
        raw-issue-ids                     (rest arguments)
        [success error-message]           (if success
                                            (if (pos? (count raw-issue-ids))
                                              [true  nil]
                                              [false "No issue numbers were provided, but at least one is required."])
                                            [success error-message])
        [success error-message issue-ids] (if success
                                            (try
                                              [true nil (distinct (map #(Long/parseLong %) raw-issue-ids))]
                                              (catch NumberFormatException nfe
                                                [false (str "Could not parse issue numbers: " (s/join ", " raw-issue-ids)) nil]))
                                            [success error-message nil])
        [success error-message issues]    (if success
                                            (try
                                              [true nil (doall (map (partial gh/issue repo-name) issue-ids))]
                                              (catch Exception e
                                                [false (str "Invalid repository (" repo-name "), and/or issue numbers (" (s/join ", " raw-issue-ids) ").") nil]))
                                            [success error-message nil])
        message                           (tem/render "issue-details.ftl"
                                                      { :success      success
                                                        :repoName     repo-name
                                                        :issueIds     issue-ids
                                                        :issues       issues
                                                        :errorMessage error-message })]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))
(def ^:private issue-details-short! "Shorthand for issue-details - see help for that command for details." issue-details!)

(declare help!)

; Table of commands - each of these must be a function of 3 args (stream-id, plain-text-of-message, words-in-message)
(def ^:private commands
  {
    "list-repos"                   #'list-repos!
    "lr"                           #'list-repos-short!
    "list-all-issues"              #'list-all-issues!
    "lai"                          #'list-all-issues-short!
    "list-open-issues"             #'list-open-issues!
    "loi"                          #'list-open-issues-short!
    "list-closed-issues"           #'list-closed-issues!
    "lci"                          #'list-closed-issues-short!
    "list-recently-updated-issues" #'list-recently-updated-issues!
    "lrui"                         #'list-recently-updated-issues-short!
    "issue-details"                #'issue-details!
    "id"                           #'issue-details-short!
    "help"                         #'help!
  })

(defn- help!
  "Displays this help message."
  [stream-id _ _]
  (sym/send-message! cnxn/symphony-connection
                     stream-id
                     (tem/render "help.ftl"
                                 { :commands (map #(vector (key %) (:doc (meta (val %)))) (sort-by key commands)) })))

(defn- process-command!
  "Looks for given command in the message text, executing it and returning true if it was found, false otherwise."
  [from-user-id stream-id text plain-text command]
  (let [words (s/split plain-text #"\s+")]
    (if (= (first words) command)
      (do
        (log/debug "Command"      command
                   "requested by" (:email-address (syu/user cnxn/symphony-connection from-user-id))
                   "in stream"    stream-id)
        ((get commands command) stream-id plain-text words)
        true)
      false)))

(defn process-commands!
  "Process any commands in the given message.  Returns true if a command (or help) was displayed, false otherwise."
  [from-user-id stream-id text entity-data]
  (if-not (s/blank? text)
    (boolean (some identity (doall (map (partial process-command! from-user-id stream-id text (sym/to-plain-text text)) (keys commands)))))
    false))
