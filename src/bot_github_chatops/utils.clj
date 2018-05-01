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

(ns bot-github-chatops.utils
  (:require [clojure.string        :as s]
            [clojure.pprint        :as pp]
            [clojure.tools.logging :as log]
            [clojure.java.io       :as io]
            [clj-time.core         :as tm]
            [clj-time.format       :as tf]))

(def ^:private human-readable-formatter
   "e.g. 2017-08-17 7:31AM UTC"
   (tf/formatter "yyyy-MM-dd h:mmaa ZZZ"))

(defn date-as-string
  "Returns the given date/time as a string, using the given formatter (defaults to human-readable-formatter if not provided)."
  ([date] (date-as-string human-readable-formatter date))
  ([formatter date]
   (tf/unparse formatter date)))

(defn now-as-string
  "Returns the current date/time as a string, using the given formatter (defaults to human-readable-formatter if not provided)."
  ([] (now-as-string human-readable-formatter))
  ([formatter] (date-as-string (tm/now))))

(defn naively-pluralise
  "Naively pluralises the given string, based on the value of the given number."
  [i s]
  (if (= i 1)
    s
    (str s "s")))

(defn interval-to-string
  [i]
  (let [im (tf/instant->map i)]
   (s/trim
     (str
       (if (pos? (:years im))
         (str (:years im) (naively-pluralise (:years im) " yr") " "))
       (if (or (pos? (:years im))
               (pos? (:months im)))
         (str (:months im) (naively-pluralise (:months im) " mth") " "))
       (if (or (pos? (:years im))
               (pos? (:months im))
               (pos? (:days im)))
         (str (:days im) (naively-pluralise (:days im) " day") " "))
       (if (or (pos? (:years im))
               (pos? (:months im))
               (pos? (:days im))
               (pos? (:hours im)))
         (str (:hours im) (naively-pluralise (:hours im) " hr") " "))
       (if (or (pos? (:years im))
               (pos? (:months im))
               (pos? (:days im))
               (pos? (:hours im))
               (pos? (:minutes im)))
         (str (:minutes im) (naively-pluralise (:minutes im) " min") " "))
       (if (or (pos? (:years im))
               (pos? (:months im))
               (pos? (:days im))
               (pos? (:hours im))
               (pos? (:minutes im))
               (pos? (:seconds im)))
         (str (:seconds im) (naively-pluralise (:seconds im) " sec") " "))))))

(def ^:private sizes ["bytes" "KB" "MB" "GB" "TB" "PB" "EB" "ZB" "YB"])

(defn size-to-string
  [sz]
  (loop [i      0
         result (double sz)]
    (if (<= result 1024)
      (format "%.2f %s" result (nth sizes i))
      (recur (inc i)
             (/ result 1024)))))

(defn zip-files!
  "Zips the given files into the zip file with the given name."
  [zip-file files-to-zip]
  (with-open [zip-os (java.util.zip.ZipOutputStream. (io/output-stream zip-file))]
    (doall
      (map #(let [file-to-zip (io/file %)
                  zip-entry   (java.util.zip.ZipEntry. (.getName file-to-zip))]
              (with-open [file-data (io/input-stream file-to-zip)]
                (.putNextEntry zip-os zip-entry)
                (io/copy file-data zip-os)
                (.closeEntry zip-os)))
           files-to-zip))))
