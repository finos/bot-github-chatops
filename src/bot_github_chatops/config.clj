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

(ns bot-github-chatops.config
  (:require [clojure.java.io       :as io]
            [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [clojure.edn           :as edn]
            [clj-time.core         :as tm]
            [clj-time.coerce       :as tc]
            [aero.core             :as a]
            [mount.core            :as mnt :refer [defstate]]))

; Because java.util.logging is a hot mess
(org.slf4j.bridge.SLF4JBridgeHandler/removeHandlersForRootLogger)
(org.slf4j.bridge.SLF4JBridgeHandler/install)

(def boot-time (tm/now))

(defstate last-reload-time
          :start (tm/now))

(defmethod a/reader 'split
  [opts tag value]
  "Adds a #split reader macro to aero - see https://github.com/juxt/aero/issues/55"
  (let [[s re] value]
    (if (and s re)
      (s/split s (re-pattern re)))))

(defstate config
          :start (if-let [config-file (:config-file (mnt/args))]
                   (a/read-config config-file)
                   (a/read-config (io/resource "config.edn"))))

(defn- assoc-if-contains
  [m contains-key new-value]
  (if (contains? m contains-key)
    (assoc m contains-key new-value)
    m))

(defstate safe-config
          :start (let [result config
                       result (assoc-in  result
                                         [:symphony-coords :trust-store]
                                         [(first (:trust-store (:symphony-coords result))) "REDACTED"])
                       result (assoc-in  result
                                         [:symphony-coords :user-cert]
                                         [(first (:user-cert (:symphony-coords result))) "REDACTED"])
                       result (if (:login (:github-coords result))
                                (assoc-in result
                                          [:github-coords :login]
                                          [(first (:login (:github-coords result))) "REDACTED"])
                                result)
                       result (if (:token (:github-coords result))
                                (assoc-in result
                                          [:github-coords :token]
                                          "REDACTED")
                                result)
                       result (update-in result
                                         [:jolokia-config]
                                         assoc-if-contains
                                         "password"
                                         "REDACTED")
                       result (update-in result
                                         [:jolokia-config]
                                         assoc-if-contains
                                         "keystorePassword"
                                         "REDACTED")]
                   result))

(def ^:private build-info
  (if-let [deploy-info (io/resource "deploy-info.edn")]
    (edn/read-string (slurp deploy-info))
    (throw (RuntimeException. "deploy-info.edn classpath resource not found - did you remember to include the 'git-info-edn' task in your build?"))))

(def git-revision
  (s/trim (:hash build-info)))

(def git-url
  (str "https://github.com/symphonyoss/bot-github-chatops/tree/" git-revision))

(def build-date
  (tc/from-date (:date build-info)))

(defn reload!
  "Reloads all of configuration for the bot.  This will briefly take the bot offline."
  []
  (log/debug "Reloading github-chatops-bot configuration...")
  (mnt/stop)
  (mnt/start)
  (log/debug "github-chatops-bot configuration reloaded."))

(defn log-files
  "Returns the names (as Strings) of all current log files, or nil if there aren't any."
  []
  (seq
    (distinct
      (remove nil?
        (flatten
          (map (fn [^ch.qos.logback.classic.Logger logger]
                 (let [appenders (iterator-seq (.iteratorForAppenders logger))]
                   (map (fn [^ch.qos.logback.core.Appender appender]
                          (if (instance? ch.qos.logback.core.FileAppender appender)
                            (.getFile ^ch.qos.logback.core.FileAppender appender)))
                        appenders)))
               (.getLoggerList ^ch.qos.logback.classic.LoggerContext (org.slf4j.LoggerFactory/getILoggerFactory))))))))
