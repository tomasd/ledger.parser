(ns ledger.parser-test
  (:require [clojure.test :refer :all]
            [ledger.parser :refer :all]
            [instaparse.core :as instaparse]
            [instaparse.transform]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.time LocalDate)))










(comment
  (parse-ledger (slurp (io/resource "drew31.ledger"))))