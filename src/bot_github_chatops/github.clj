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
          :start (:github-coords cfg/config))

(defstate org
          :start (:org github-config))

(defstate opts
          :start (into { :throw-exceptions true
                         :all-pages        true
                         :per-page         100
                         :user-agent       "GitHub ChatOps Symphony Bot" }
                       (if-let [token (:token github-config)]
                         { :oauth-token token }
                         { :auth (str (first (:login github-config)) ":" (second (:login github-config))) })))

(defstate mask-private-repos?
          :start (boolean (:mask-private-repos github-config)))

(defstate masked-repos
          :start (if-let [masked-repos (seq (:masked-repos github-config))]
                   (set masked-repos)))

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
    (remove #(masked-repo? (:name %)) result)))

(defn open-issues
  "Lists the open issues in the given repo."
  [repo-name]
  (if-not (masked-repo? repo-name)
    (ti/issues org repo-name opts)
    (throw (RuntimeException. (str "Invalid repository " repo-name)))))

(defn issue
  "Gets the details of a single issue in the given repo, including comments."
  [repo-name issue-id]
  (if-not (masked-repo? repo-name)
    (assoc (ti/specific-issue org repo-name issue-id opts)
           :comment_data (ti/issue-comments org repo-name issue-id opts))   ; We use an underscore in the key to keep Freemarker happy
    (throw (RuntimeException. (str "Invalid repository " repo-name)))))
