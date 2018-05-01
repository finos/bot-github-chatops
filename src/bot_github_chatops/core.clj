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

(ns bot-github-chatops.core
  (:require [clojure.string                    :as s]
            [clojure.tools.logging             :as log]
            [mount.core                        :as mnt :refer [defstate]]
            [clj-symphony.stream               :as sys]
            [clj-symphony.message              :as sym]
            [bot-github-chatops.connection     :as cnxn]
            [bot-github-chatops.commands       :as cmd]
            [bot-github-chatops.admin-commands :as adm]))

(defn- process-message!
  "Processes all messages received by the bot."
  [{:keys [message-id timestamp stream-id user-id type text entity-data]}]
  (try
    (log/debug "Received message" message-id "from user" user-id "in stream" stream-id ":" text)
    (cmd/process-commands! user-id stream-id text entity-data)
    (adm/process-commands! user-id stream-id text entity-data)
    (catch Exception e
      (log/error e "Unexpected exception while processing message" message-id))))

(defstate github-chatops-bot-listener
          :start (sym/register-listener!   cnxn/symphony-connection process-message!)
          :stop  (sym/deregister-listener! cnxn/symphony-connection github-chatops-bot-listener))

