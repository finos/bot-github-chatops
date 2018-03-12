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

(ns bot-github-chatops.connection
  (:require [clojure.string               :as s]
            [clojure.tools.logging        :as log]
            [mount.core                   :as mnt :refer [defstate]]
            [clj-symphony.connect         :as syc]
            [clj-symphony.user            :as syu]
            [clj-symphony.user-connection :as syuc]
            [bot-github-chatops.config    :as cfg]))

(defstate symphony-connection
          :start (let [cnxn (syc/connect (:symphony-coords cfg/config))
                       bot  (syu/user cnxn)
                       _    (log/info (str "Connected to Symphony pod " (:company bot) " v" (syc/version cnxn) " as " (:display-name bot) " (" (:email-address bot) ")"))]
                    cnxn))

(defstate bot-user
          :start (syu/user symphony-connection))

(defstate admins
          :start (let [result (map (partial syu/user symphony-connection) (:admin-emails cfg/config))]
                   (if (pos? (count result))
                     (do
                       (log/info "Admins:" (s/join ", " (map :email-address result)))
                       result)
                     (log/info "No admins configured - Admin ChatOps interface will not be available."))))

(defn is-admin?
  "Is the given user an admin of the bot?"
  [u]
  (let [user-id (syu/user-id u)]
    (some identity (map #(= user-id (syu/user-id %)) admins))))

(defstate accept-connections-interval-ms
          :start (if-let [accept-connections-interval (:accept-connections-interval cfg/config)]
                   (* 1000 60 accept-connections-interval)    ; Convert to ms
                   (* 1000 60 30)))                           ; If not specified, default to 30 minutes

(defn- accept-all-connection-requests-and-log
  "Unconditionally accepts all incoming connection requests, and logs the number accepted."
  [connection]
  (let [accept-count (syuc/accept-all-connection-requests! connection)]
    (log/info (str "Accepted " accept-count " incoming connection requests."))))

(defstate accept-connections-job
          :start (let [p (promise)]
                   (future
                     (accept-all-connection-requests-and-log symphony-connection)  ; Accept upon startup
                     (try
                       (while (= :timedout (deref p accept-connections-interval-ms :timedout))
                         (accept-all-connection-requests-and-log symphony-connection))
                       (catch InterruptedException ie)  ; Ignore interrupted exceptions
                       (catch Exception e
                         (log/warn e))))
                   #(deliver p :stopped))
          :stop  (accept-connections-job))
