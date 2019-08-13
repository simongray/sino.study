(ns sinostudy.pinyin.core-test
  (:require [clojure.test :refer :all]
            [sinostudy.pinyin.core :refer :all]))

(deftest test-umlaut
  (testing "umlaut"
    (is (= (with-umlaut "VvÜü") "ÜüÜü"))))

;; only tests a single char for now!
(deftest test-diacritic
  (testing "diacritic"
    (testing "added to characters?"
      (are [x y] (= x y)
                 \a (diacritic \a 0)
                 \ā (diacritic \a 1)
                 \á (diacritic \a 2)
                 \ǎ (diacritic \a 3)
                 \à (diacritic \a 4)
                 \a (diacritic \a 5)
                 \A (diacritic \A 0)
                 \Ā (diacritic \A 1)
                 \Á (diacritic \A 2)
                 \Ǎ (diacritic \A 3)
                 \À (diacritic \A 4)
                 \A (diacritic \A 5)))
    (testing "tone out of range?"
      (is (thrown? IndexOutOfBoundsException (diacritic \a 6))))
    (testing "string instead of char?"
      (is (nil? (diacritic "a" 1))))))

(deftest test-diacritic-index
  (testing "diacritic-index"
    (testing "a-rule"
      (is (= (diacritic-index "ao1") 0))
      (is (= (diacritic-index "lang4") 1))
      (is (= (diacritic-index "quan") 2)))
    (testing "e-rule"
      (is (= (diacritic-index "eng") 0))
      (is (= (diacritic-index "heng1") 1))
      (is (= (diacritic-index "zheng") 2)))
    (testing "ou-rule"
      (is (= (diacritic-index "ou") 0))
      (is (= (diacritic-index "tou2") 1))
      (is (= (diacritic-index "zhou") 2)))
    (testing "general rule"
      (is (= (diacritic-index "e") 0))
      (is (= (diacritic-index "eng") 0))
      (is (= (diacritic-index "long2") 1))
      (is (= (diacritic-index "lan") 1))
      (is (= (diacritic-index "kuo4") 2)))
    (testing "mixed case"
      (is (= (diacritic-index "WANG") 1))
      (is (= (diacritic-index "lI0") 1))
      (is (= (diacritic-index "Qu4") 1)))
    (testing "undefined cases (returns nil)"
      (is (thrown? NullPointerException (diacritic-index nil)))
      (is (nil? (diacritic-index "")))
      (is (nil? (diacritic-index "4")))
      (is (nil? (diacritic-index [1 2 3])))
      (is (nil? (diacritic-index {:foo :bar})))
      (is (nil? (diacritic-index {:foo :bar}))))))

(deftest test-digit->diacritic
  (testing "digit->diacritic"
    (testing "converts properly?"
      (is (= (digit->diacritic "long3") "lǒng"))
      (is (= (digit->diacritic "er2") "ér")))
    (testing "exceptions"
      (is (thrown? NumberFormatException (digit->diacritic "long")))
      (is (thrown? NumberFormatException (digit->diacritic "")))
      (is (thrown? ClassCastException (digit->diacritic [])))
      (is (thrown? ClassCastException (digit->diacritic [1 2 3])))
      (is (thrown? ClassCastException (digit->diacritic 0)))
      (is (thrown? ClassCastException (digit->diacritic \a))))))

(deftest test-digits->diacritics
  (testing "digits->diacritics"
    (testing "converts properly?"
      (is (= (digits->diacritics "ni3hao3, ni3 shi4 shei2?") "nǐhǎo, nǐ shì shéi?"))
      (is (= (digits->diacritics "long") "long"))
      (is (= (digits->diacritics "") "")))
    (testing "non-strings"
      (is (= (digits->diacritics []) []))
      (is (= (digits->diacritics [1 2 3]) [1 2 3]))
      (is (= (digits->diacritics 0) 0))
      (is (= (digits->diacritics \a) \a)))))
