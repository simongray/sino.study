(ns sinostudy.config)

;; Allows for certain constants to be defined at compile time,
;; e.g. if debug? is false the production URI should be used.
;; See: :closure-defines in project.clj
(def debug?
  ^boolean goog.DEBUG)
