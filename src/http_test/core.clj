(ns http-test.core
  (:require [clojure.core.async :as async :refer [go put! <!]]
            [clj-http.client :as client]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn get-page [^String url ^Integer to]
  (let [toch (async/timeout to)
        ch   (go (client/get url))]
    (go (first (async/alts! [toch ch])))))

(defn timed-f [f]
  (go (let [start (java.lang.System/nanoTime)
            res (async/<! (f))]
        [(/ (- (java.lang.System/nanoTime) start) 1000000.0) res])))

(defn- to-seq [c]
  (async/go-loop [v (<! c) l []]
                 (if v
                   (recur (<! c) (conj l v))
                   l)))

(defn- run-times [times f]
  (let [chfrom (async/to-chan (range times))
        chto (async/chan)]
    (async/pipeline-async
      4
      chto
      (fn [_ ch]
        (async/pipe (timed-f f) ch))
      chfrom)
    chto))

(defn do-times [times url timeout]
  (let [f (partial get-page url timeout)
        c (run-times times f)]
    (->> (async/<!! (to-seq c))
         (filter second)
         (map first))))

(defn std-dev [ns]
  (let [c (count ns)
        avg (/ (apply + ns) c)
        diffs (map #(* (- % avg) (- % avg)) ns)
        sum (apply + diffs)]
    (Math/sqrt (/ sum c))))

(defn stats [recs expected]
  (let [c (count recs)]
    {:total-tests expected
     :max (apply max recs)
     :min (apply min recs)
     :failed (float (/ (- expected c) expected))
     :average (/ (apply + recs) c)
     :std-dev (std-dev recs)}))

(defn fix-url [url]
  (if (re-find #"^https?:\/\/" url)
    url
    (str "http://" url)))

(def cli-options
  [["-n" "--count COUNT" "Total number of requests to make"
   :default 10
   :parse-fn #(Integer/parseInt %)]
  ["-t" "--timeout MS" "Total timeout for requests in milliseconds"
   :default 5000
   :parse-fn #(Integer/parseInt %)]
  ["-h" "--help"]])

(defn format-stats [stats]
  (format "total: %d, min: %.2fms, max: %.2fms, avg: %.2fms, failed: %.1f%%, std-dev: %.2f"
          (:total-tests stats)
          (:min stats) (:max stats)
          (:average stats) (* 100 (:failed stats))
          (:std-dev stats)))

(defn run-with-params [url c to]
  (println "Running tests: ")
  (println "        url:" url)
  (println "total tests:" c)
  (println "    timeout:" to)
  (println "")
  (let [url (fix-url url)
        res (stats (do-times c url to) c)]
    (println (format-stats res))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (cond
      (:help opts) (println (:summary opts))
      (not (seq (:arguments opts))) (println "Please supply an HTTP URL to test with")
      :else (run-with-params (first (:arguments opts))
                             (get-in opts [:options :count])
                             (get-in opts [:options :timeout])))))
