(ns wayback.core
  (:use [clj-xpath.core :only [$x $x:tag $x:text $x:attrs $x:attrs* $x:node]])
  (:import java.io.File)
  (:import java.util.Date)
  (:import java.util.GregorianCalendar)
  )

(def ms-diff
  (-
   (.getTime (.getTime (GregorianCalendar. 2013 7 18)))
   (.getTime (.getTime (GregorianCalendar. 2010 6 28)))))

(defn get-post-filenames
  [dir]
  (filter (fn [x] (re-find #"/\d+.xml$" x))
          (for [file (file-seq (File. dir))] (str dir "/" (.getName file)))))

(defn munge-namespace
  [some-string]
  (clojure.string/replace
   (clojure.string/replace some-string "<wp:" "<wp_")
   "</wp:" "</wp_"))

(defn retrieve-post-data
  [xml-data]
  {
   :timestamp (date-str-to-timestamp ($x:text "//pubDate" xml-data))
   :title ($x:text "//title" xml-data)
   :id ($x:text "//wp_post_id" xml-data)
   })

(defn date-str-to-timestamp
  [date-str]
  (def df (java.text.SimpleDateFormat. "EEE MMM d HH:mm:ss zzz yyyy"))
  (.getTime (.parse df date-str)))

(defn get-desired-timestamp
  []
  (-  (.getTime (Date.))
      ms-diff
      ( * 7 24 3600 1000)))


(defn get-post
  [filename]
  (-> filename slurp munge-namespace retrieve-post-data)
  )

(defn get-post-data
  [dir]
  (map get-post (get-post-filenames dir)))

(defn get-closest-posts
  [dir]
  (take 5
   (filter
    (fn [x] (> (:timestamp x) (get-desired-timestamp)))
    (sort-by :timestamp (get-post-data dir)))))

(defn format-post
  [post]
  (def ymd (java.text.SimpleDateFormat. "yyyy MMM d"))
  (format
   "* [%s] %s"
   (.format ymd (Date. (:timestamp post)))
   (:title post)))
