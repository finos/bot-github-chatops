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
            [bot-github-chatops.config :as cfg]))

(defstate github-config
          :start (:github-coords cfg/config))

(defstate org
          :start (:org github-config))

(defstate mask-private-repos?
          :start (:mask-private-repos github-config))

(defstate masked-repos
          :start (set (:masked-repos github-config)))

(defstate opts
          :start (into { :all-pages  true
                         :per-page   100
                         :user-agent "GitHub ChatOps Symphony Bot" }
                       (if-let [token (:token github-config)]
                         {:oauth-token token}
                         {:auth (str (first (:login github-config)) ":" (second (:login github-config)))})))

(defn repos
  "Lists the repositories in org (with masked repos elided)."
  []
  (let [all-repos  (tr/org-repos org opts)
        tmp        (if mask-private-repos?
                     (remove :private all-repos)
                     all-repos)
        repo-names (map :name tmp)]
    (sort (remove #(contains? masked-repos %) repo-names))))


(defn repo-url
  "Returns the URL of the given repository."
  [repo-name]
  (str "https://github.com/" org "/" repo-name))
