(ns wayback.core
  (:use [clj-xpath.core :only [$x $x:tag $x:text $x:attrs $x:attrs* $x:node]])
  (:import java.io.File)
  (:import java.util.Date)
  (:import java.util.GregorianCalendar)
  (:gen-class))

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
  (->
   some-string
   (.replace "<wp:" "<wp_")
   (.replace "</wp:" "</wp_")
   (.replace "<excerpt:" "<excerpt_")
   (.replace "</excerpt:" "</excerpt_")))

(defn date-str-to-timestamp
  [date-str]
  (def df (java.text.SimpleDateFormat. "EEE MMM d HH:mm:ss zzz yyyy"))
  (.getTime (.parse df date-str)))

(defn retrieve-post-data
  [xml-data]
  {
   :timestamp (date-str-to-timestamp ($x:text "//pubDate" xml-data))
   :title ($x:text "//title" xml-data)
   :excerpt ($x:text "//excerpt_encoded" xml-data)
   :slug ($x:text "//wp_post_name" xml-data)
   :id ($x:text "//wp_post_id" xml-data)
   })

(defn get-desired-timestamp
  []
  (- (.getTime (Date.))
      ms-diff))

(defn get-post
  [filename]
  (-> filename slurp munge-namespace retrieve-post-data)
  )

(defn get-all-posts
  [dir]
  (map get-post (get-post-filenames dir)))

(defn get-closest-posts
  [dir timestamp]
  (let
      [posts (sort-by :timestamp (get-all-posts dir))]
    (concat
     (take-last 5
                (filter
                 (fn [x] (< (:timestamp x) timestamp))
                 posts))
     (take 5
           (filter
            (fn [x] (>= (:timestamp x) timestamp))
            posts)))))

(defn format-post
  [post domain]
  (def ymd (java.text.SimpleDateFormat. "yyyy MMM d"))
  (format
   "%s: %s | %s\n%s"
   (.format ymd (Date. (:timestamp post)))
   (:title post)
   (:excerpt post)
   (str "http://" domain "/" (:slug post))
   ))

(defn output-str-for-site
  [dir domain]
  (clojure.string/join
   "\n"
   (map (fn [x] (format-post x domain))
        (get-closest-posts dir (get-desired-timestamp)))))

#(println
 (output-str-for-site "data/liao-yung" "liao-yung.posthaven.com")
 (output-str-for-site "data/milni" "milni.posthaven.com"))

(defn -main [& args]
  (println (output-str-for-site (first args) (second args))))
