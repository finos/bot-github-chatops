;
; Copyright 2017 Fintech Open Source Foundation
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
  [_ stream-id _ _]
  (let [message (tem/render "list-repos.ftl"
                            { :repos (gh/repos) })]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))


(defn- list-issues!
  "Lists issues for the given repository in the given state."
  [stream-id words summary command-name issue-filter]
  (let [arguments                      (rest words)
        repo-slug                      (first arguments)
        [success error-message]        (if-not (s/blank? repo-slug)
                                         [true  nil]
                                         [false "No repository was provided, but it is required."])
        [success error-message issues] (if success
                                         (try
                                           [true nil (gh/issues repo-slug issue-filter)]
                                           (catch Exception e
                                             [false (str "Invalid repository '" repo-slug "'.") nil]))
                                         [success error-message nil])
        message                        (tem/render "list-issues.ftl"
                                                   { :success      success
                                                     :repoSlug     repo-slug
                                                     :summary      summary
                                                     :commandName  command-name
                                                     :issues       issues
                                                     :errorMessage error-message } )]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))


(defn- list-all-issues!
  "Lists all issues in the given repository, which must be supplied immediately after the command e.g. list-all-issues MyRepository"
  [_ stream-id _ words]
  (list-issues! stream-id words "all" "list-all-issues" { :state "all" }))


(defn- list-open-issues!
  "Lists open issues in the given repository, which must be supplied immediately after the command e.g. list-open-issues MyRepository"
  [_ stream-id _ words]
  (list-issues! stream-id words "open" "list-open-issues" { :state "open" }))


(defn- list-closed-issues!
  "Lists closed issues in the given repository, which must be supplied immediately after the command e.g. list-closed-issues MyRepository"
  [_ stream-id _ words]
  (list-issues! stream-id words "closed" "list-closed-issues" { :state "closed" }))


(defn- list-recently-updated-issues!
  "Lists issues updated in the last month in the given repository, which must be supplied immediately after the command e.g. list-recently-updated-issues MyRepository"
  [_ stream-id _ words]
  (list-issues! stream-id words "recently updated" "list-recently-updated-issues" { :state "all" :since (tf/unparse iso-date-formatter (tm/minus (tm/now) (tm/months 1))) }))


(defn- issue-details!
  "Displays details on one or more issues in a given repository. The repository name must be supplied immediately after the command, followed by one or more issue ids e.g. issue-details MyRepository 14 17 22"
  [_ stream-id _ words]
  (let [arguments                         (rest words)
        repo-slug                         (first arguments)
        [success error-message]           (if-not (s/blank? repo-slug)
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
                                              [true nil (doall (map (partial gh/issue repo-slug) issue-ids))]
                                              (catch Exception e
                                                [false (str "Invalid repository (" repo-slug "), and/or issue numbers (" (s/join ", " raw-issue-ids) ").") nil]))
                                            [success error-message nil])
        message                           (tem/render "issue-details.ftl"
                                                      { :success      success
                                                        :repoSlug     repo-slug
                                                        :issueIds     issue-ids
                                                        :issues       issues
                                                        :errorMessage error-message })]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))


(defn- add-comment!
  "Adds a comment to an issue in a given repository. The repository name must be supplied immediately after the command, followed by a single issue id, followed by the comment e.g. add-comment MyRepository 42 Can you please provide the log files?"
  [from-user-id stream-id _ words]
  (let [arguments                         (rest words)
        repo-slug                         (first arguments)
        [success error-message]           (if-not (s/blank? repo-slug)
                                            [true  nil]
                                            [false "No repository was provided, but it is required."])
        raw-issue-id                      (first (rest arguments))
        [success error-message issue-id]  (if success
                                            (if-not (s/blank? raw-issue-id)
                                              (try
                                                [true nil (Long/parseLong raw-issue-id)]
                                                (catch NumberFormatException nfe
                                                  [false (str "Could not parse issue number: " raw-issue-id) nil]))
                                              [false "No issue number was provided, but it is required." nil])
                                            [success error-message nil])
        comment-text                      (s/join " " (rest (rest arguments)))
        [success error-message]           (if success
                                            (if-not (s/blank? comment-text)
                                              (try
                                                (let [from-user      (syu/user cnxn/symphony-connection from-user-id)
                                                      github-comment (tem/render "add-comment-github.ftl"
                                                                                 { :displayName  (:display-name from-user)
                                                                                   :userId       from-user-id
                                                                                   :message      comment-text })]
                                                  (gh/add-comment repo-slug
                                                                  issue-id
                                                                  github-comment)
                                                  [true nil])
                                                (catch Exception e
                                                  [false (str "Invalid repository (" repo-slug "), and/or issue number (" raw-issue-id ").")]))
                                              [false "No comment was provided, but it is required."])
                                            [success error-message])
        message                           (tem/render "add-comment.ftl"
                                                      { :success      success
                                                        :repoSlug     repo-slug
                                                        :issueId      issue-id
                                                        :errorMessage error-message })]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       message)))


(declare help!)

; Table of commands - each of these must be a function of 4 args (from-user-id, stream-id, plain-text-of-message, words-in-message)
(def ^:private commands
  {
    "list-repos"                   #'list-repos!
    "lr"                           #'list-repos!
    "list-all-issues"              #'list-all-issues!
    "lai"                          #'list-all-issues!
    "list-open-issues"             #'list-open-issues!
    "loi"                          #'list-open-issues!
    "list-closed-issues"           #'list-closed-issues!
    "lci"                          #'list-closed-issues!
    "list-recently-updated-issues" #'list-recently-updated-issues!
    "lrui"                         #'list-recently-updated-issues!
    "issue-details"                #'issue-details!
    "id"                           #'issue-details!
    "add-comment"                  #'add-comment!
    "ac"                           #'add-comment!
    "help"                         #'help!
  })

(defn- help!
  "Displays this help message."
  [_ stream-id _ _]
  (let [commands-in-help-format (sort-by first
                                         (map #(vec [(s/join ", " (sort-by (fn [s] (- (count s)))    ; Join all commands into a single string, sorted by length descending (so that we get the long name first)
                                                                           (map first (second %))))
                                                     (:doc (meta (first %)))])
                                              (group-by val commands)))]
    (sym/send-message! cnxn/symphony-connection
                       stream-id
                       (tem/render "help.ftl"
                                   { :commands commands-in-help-format }))))

(defn- process-command!
  "Looks for given command in the message text, executing it and returning true if it was found, false otherwise."
  [from-user-id stream-id text plain-text command]
  (let [words (s/split plain-text #"\s+")]
    (if (= (s/lower-case (first words)) command)
      (do
        (log/debug "Command"      command
                   "requested by" (:email-address (syu/user cnxn/symphony-connection from-user-id))
                   "in stream"    stream-id)
        ((get commands command) from-user-id stream-id plain-text words)
        true)
      false)))

(defn process-commands!
  "Process any commands in the given message.  Returns true if a command (or help) was displayed, false otherwise."
  [from-user-id stream-id text entity-data]
  (if-not (s/blank? text)
    (boolean (some identity (doall (map (partial process-command! from-user-id stream-id text (s/trim (sym/to-plain-text text))) (keys commands)))))
    false))
