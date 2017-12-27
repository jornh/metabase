(ns metabase.postapi-test
  (:require [expectations :refer :all]
            [metabase.postapi :as postapi]))


;; skip schema
(expectations/expect
  "apihost:8000"
  (postapi/a "schema://apihost:3000"))
