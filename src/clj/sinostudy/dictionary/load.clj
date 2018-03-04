(ns sinostudy.dictionary.load
  (:require [clojure.java.io :as io]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.entry :as entry]
            [clojure.string :as str]))

;;;; CC-CEDICT

(defn load-cedict
  "Load the listings of a CC-CEDICT dictionary file into Clojure maps."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter #(not (str/starts-with? % "#")))
         (map entry/line->listing)
         (vec))))


;;;; WORD FREQUENCY

(defn line->freq-listing
  "Extract the constituents of a line in a CC-CEDICT dictionary file.
  Returns a map representation suitable for use as a dictionary entry."
  [line]
  (let [re #"^([^ ]+) ([^ ]+) ([^ ]+)"
        [_ n freq word :as entry] (re-matches re line)]
    (when entry
      (let []
        {::d/rank      (Integer/parseInt n)
         ::d/frequency (Double/parseDouble freq)
         ::d/word      word}))))

(defn normalise
  "Normalise the frequency of a freq-listing."
  [max-freq freq-listing]
  (assoc freq-listing ::d/frequency (/ (::d/frequency freq-listing)
                                       max-freq)))

(defn load-frequency
  "Load the listings of a frequency file into Clojure maps."
  ([file]
   (with-open [reader (io/reader file)]
     (let [raw-listings (->> (line-seq reader)
                             (filter #(re-find #"^\d+ " %))
                             (map line->freq-listing))
           max-freq (::d/frequency (first raw-listings))]
       (->> raw-listings
            (map (partial normalise max-freq))
            (vec))))))
