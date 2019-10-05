(ns sinostudy.spec.pages
  (:require [clojure.spec.alpha :as s]))

(s/def ::category
  #{:term :static})

(s/def ::page
  (s/tuple ::category string?))
