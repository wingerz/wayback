(ns wayback.core
  (:use [clj-xpath.core :only [$x $x:tag $x:text $x:attrs $x:attrs* $x:node]])
  (:import java.io.File)
  )

(defn get-post-filenames
  [dir]
  (filter (fn [x] (.endsWith x ".xml"))
          (for [file (file-seq (File. dir))] (str dir "/" (.getName file)))))

(defn munge-namespace
  [some-string]
  (clojure.string/replace
   (clojure.string/replace some-string "<wp:" "<wp_")
   "</wp:" "</wp_"))

(defn retrieve-post-data
  [xml-data]
  {
   :date ($x:text "//pubDate" xml-data)
   :title ($x:text "//title" xml-data)
   :id ($x:text "//wp_post_id" xml-data)
   })

(defn get-post-data
  [dir]
  (map (fn[x] (-> x slurp munge-namespace retrieve-post-data))  (get-post-filenames dir)))