(ns vk-audio-fetch.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.io File))
  (:gen-class))

(defn do-req [token uid]
  (-> (client/get "https://api.vk.com/method/audio.get"
                  {:query-params {:owner_id     uid
                                  :access_token token
                                  }})
      :body
      json/read-str))

(defn get-list [resp]
  (-> resp
      (get "response")
      rest))

(defn build-file-name [item]
  (-> (str (get item "artist")
           " - "
           (get item "title")
           ".mp3")
      (clojure.string/replace #"/" "|")))

(defn map-list [list]
  (map (fn [i] {:file (build-file-name i)
                :url  (get i "url")}) list))

(defn download [url file]
  (with-open [in (io/input-stream (io/as-url url))]
    (io/copy in (File. file)))
  (println file)
  file)

(defn get-downloaded [path]
  (->> (File. path)
       .listFiles
       vec
       (map #(.getName %))
       set))

(defn skip-downloaded [path lst]
  (let [downloaded (get-downloaded path)]
    (filter #(not (downloaded (:file %))) lst)))

(defn get-user-audio [token uid path]
  (->> (do-req token uid)
       get-list
       map-list
       (skip-downloaded path)
       (map #(download (:url %) (str path "/" (:file %))))
       count
       (str "files: ")))

(defn -main
  [token uid target-dir]
  (println (get-user-audio token uid target-dir)))
