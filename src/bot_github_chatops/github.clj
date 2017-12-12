;
; Copyright © 2017 Symphony Software Foundation
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
(ns bot-github-chatops.github
  (:require [clojure.string            :as s]
            [clojure.core.memoize      :as memo]
            [clojure.tools.logging     :as log]
            [mount.core                :as mnt :refer [defstate]]
            [tentacles.core            :as tc]
            [tentacles.repos           :as tr]
            [tentacles.orgs            :as to]
            [tentacles.users           :as tu]
            [tentacles.issues          :as ti]
            [bot-github-chatops.config :as cfg]))

(defstate github-config
          :start (if-let [result (:github-coords cfg/config)]
                   result
                   (throw (RuntimeException. "GitHub configuration is mandatory, but is missing."))))

(defstate org
          :start (let [result (:org github-config)]
                   (if-not (s/blank? result)
                     result
                     "symphonyoss")))

(defstate opts
          :start (into { :throw-exceptions true
                         :all-pages        true
                         :per-page         100
                         :user-agent       "GitHub ChatOps Symphony Bot" }
                       (if-let [token (:token github-config)]
                         { :oauth-token token }
                         (let [login    (first (:login github-config))
                               password (second (:login github-config))]
                           (if (and login password)
                             { :auth (str login ":" password) }
                             (throw (RuntimeException. "GitHub credentials missing from configuration.")))))))

(defstate mask-private-repos?
          :start (if-let [from-config (:mask-private-repos github-config)]
                   (boolean from-config)
                   true))

(defstate masked-repos
          :start (if-let [from-config (seq (:masked-repos github-config))]
                   (set from-config)))

(defn private-repo?
  "Is the given repository private?"
  [repo-name]
  (:private (tr/specific-repo org repo-name opts)))

(defn- masked-repo-fn?
  "Is the given repository masked?"
  [repo-name]
  (or (contains? masked-repos repo-name)
      (and mask-private-repos? (private-repo? repo-name))))
(def ^:private masked-repo? (memo/lru masked-repo-fn? :lru/threshold 500))   ; Memoize so as to reduce GH API calls

; We do this so that configuration reloads via the bot's admin command interface force a flush of the masked-repo? cache.
(defstate masked-repo-cache
          :start (memo/memo-clear! masked-repo?))

(defn repos
  "Lists the non-masked repositories in the configured org."
  []
  (let [result (tr/org-repos org opts)]
    (doall (remove #(masked-repo? (:name %)) result))))

(defn issues
  "Lists issues in the given repo, optionally including these filtering and sorting options:

    milestone      - milestone number, or '*' for all, or 'none' for issues without milestones
    state          - 'open', 'closed', or 'all'
    assignee       - GitHub user id or '*' for any, or 'none' for issues without an assignee
    creator        - GitHub user id
    mentioned      - GitHub user id
    labels         - comma-delimited list of label names
    sort-field     - 'created', 'updated', or 'comments'
    sort-direction - 'asc' or 'desc'
    since          - issues updated since this date (formatted as an ISO 8601 string: 'YYYY-MM-DDTHH:MM:SSZ')"
  ([repo-name] (issues repo-name nil))
  ([repo-name filters]
   (if-not (masked-repo? repo-name)
     (ti/issues org repo-name (into opts filters))
     (throw (RuntimeException. (str "Invalid repository '" repo-name "'."))))))

(defn issue
  "Gets the details of a single issue in the given repo, including comments."
  [repo-name issue-id]
  (if-not (masked-repo? repo-name)
    (assoc (ti/specific-issue org repo-name issue-id opts)
           :comment_data (ti/issue-comments org repo-name issue-id opts))
    (throw (RuntimeException. (str "Invalid repository " repo-name)))))


(defn add-comment
  "Adds the given content as a comment to the given issue."
  [repo-name issue-id comment-content]
  (if-not (masked-repo? repo-name)
    (ti/create-comment org repo-name issue-id comment-content opts)
    (throw (RuntimeException. (str "Invalid repository " repo-name)))))

