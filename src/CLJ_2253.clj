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

; Workaround for https://dev.clojure.org/jira/browse/CLJ-2253

(ns CLJ-2253
  (:require [clojure.java.io :as io]))

(defmacro base64-encode
  [^String s]
  (let [jvm-version (System/getProperty "java.version")]
    (if (or (clojure.string/starts-with? jvm-version "1.6")
            (clojure.string/starts-with? jvm-version "1.7"))
      `(javax.xml.bind.DatatypeConverter/printBase64Binary (.getBytes ~s))
      `(.encodeToString (java.util.Base64/getEncoder) (.getBytes ~s)))))

(defn- open-input-stream [^java.net.URL url]
  (if-let [user-info (.getUserInfo url)]
    (let [uc    (.openConnection url)
          creds (base64-encode user-info)
          basic (str "Basic " creds)]
      (.setRequestProperty uc "Authorization" basic)
      (.getInputStream uc))
    (.openStream url)))

(extend java.net.URL
  io/IOFactory
  (assoc io/default-streams-impl
    :make-input-stream (fn [^java.net.URL x opts]
                         (io/make-input-stream
                          (if (= "file" (.getProtocol x))
                            (java.io.FileInputStream. (io/as-file x))
                            (open-input-stream x)) opts))
    :make-output-stream (fn [^java.net.URL x opts]
                          (if (= "file" (.getProtocol x))
                            (io/make-output-stream (io/as-file x) opts)
                            (throw (IllegalArgumentException. (str "Can not write to non-file URL <" x ">")))))))
