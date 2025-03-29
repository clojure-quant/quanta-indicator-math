(ns demo.env
  (:require
   [clojure.string :as str]
   [tablecloth.api :as tc]
   [clojure.java.io :as java-io]
   [tech.v3.io :as io]
   [tech.v3.dataset.print :as p])
  (:import (java.io FileNotFoundException)))

(defn save-ds [filename ds]
  (let [filename (str "data/" filename ".nippy.gz")
        s (io/gzip-output-stream! filename)]
    (io/put-nippy! s ds)))

(defn load-ds [filename]
  (let [filename (str "data/" filename ".nippy.gz")
        s (io/gzip-input-stream filename)
        ds (io/get-nippy s)]
    ds))


(defn print-ds [ds]
  (-> ds
      (p/print-range :all)))

(defn spit-ds [ds-all dsname]
  (->> (p/dataset->str ds-all {:print-index-range :all})
       (spit (str "study/" dsname ".txt"))))