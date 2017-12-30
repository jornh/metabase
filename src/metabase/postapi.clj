(ns metabase.postapi)

(first
  (clojure.string/split
    (second
      (clojure.string/split
        "foo:bar/aaa"
        #":"))
   #"/"))

(def mystr "foo://bar:8000/baz?wee")

(first
  (clojure.string/split
    (second (clojure.string/split mystr #"://")) ; strip schema before
   #"/")) ; strip path after

(def a
  (first
    (clojure.string/split
      (second (clojure.string/split mystr #"://"))  ; strip schema before
     #"/")))                                        ; strip path after

(eval a)
