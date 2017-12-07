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

(defstate mask-private-repos?
          :start (:mask-private-repos github-config))

(defstate masked-repos
          :start (if-let [result (seq (:masked-repos github-config))]
                   (set result)))

(defstate opts
          :start (into { :throw-exceptions true
                         :all-pages        true
                         :per-page         100
                         :user-agent       "GitHub ChatOps Symphony Bot" }
                       (if-let [token (:token github-config)]
                         {:oauth-token token}
                         {:auth (str (first (:login github-config)) ":" (second (:login github-config)))})))

(defn repos
  "Lists the repositories in org (with masked repos elided)."
  []
  (let [result (tr/org-repos org opts)
        result (if mask-private-repos?
                 (remove :private result)
                 result)
        result (if masked-repos
                 (remove #(contains? masked-repos (:name %)) result)
                 result)]
    result))


(defn issues
  "Lists the issues in the given repo."
  [repo-name]
  (ti/issues org repo-name))
