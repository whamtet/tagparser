(ns tagparser.names)

(def names (-> (slurp "names.txt") (.split "\n")))
(def good-names
  (for [name names :when (.contains name "-")]
    (-> (.split name " -" first))))
