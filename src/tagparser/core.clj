(ns tagparser.core)
(import 'java.io.File)

;TEXT FILE REMEMBER!!!
(def f (File. "/Users/matthewmolloy/workspace/chunkmapper2/horse.txt"))

(def lines (-> f slurp (.split "\n")))

(def tag-types ["TAG_Byte_Array" "TAG_Int_Array"
                "TAG_Byte" "TAG_Short" "TAG_Int" "TAG_Long"
                "TAG_Float" "TAG_Double" "TAG_String"
                "TAG_List" "TAG_Compound"])

(def java-types [nil nil
                 "ByteTag" "ShortTag" "IntTag" "LongTag"
                 "FloatTag" "DoubleTag" "StringTag"
                 "ListTag" "CompoundTag"])

(def get-java-type (zipmap tag-types java-types))

(defn get-type [line]
  (some identity (for [k tag-types]
                   (if (.startsWith (.trim line) k) k))))

(def leaf-tags #{"TAG_Byte" "TAG_Short" "TAG_Int" "TAG_Long" "TAG_Float"
                 "TAG_Double" "TAG_String" })
(def comment-tags #{"TAG_Int_Array" "TAG_Byte_Array"})
(def branch-tags #{"TAG_List" "TAG_Compound"})

(defn my-name [k]
  (if k (name k)))
(defn split [s on]
  (if s (.split s on)))

(def locals (atom {}))

(defn affix-number [name]
    (if-not (contains? @locals name)
      (swap! locals #(assoc % name 0)))
    (swap! locals #(update-in % [name] inc))
    (str name (@locals name)))

(def stack (atom []))
;{:type :name :numbered-name :generic}

(def code-lines
  (filter identity
          (for [line (butlast lines)]
            (let [
                  type (get-type line)
                  name (or (if (empty? @stack) "root"
                           (-> (re-find #"\"(\w+)\"" line) second)) "anon")
                  numbered-name (affix-number name)
                  value (-> line (.split ": ") second)
                  value (cond
                         (= "TAG_String" type) (str \" value \")
                         (= "TAG_Long" type) (str (.trim value) "L")
                         (= "TAG_Short" type) (str "(short) " value)
                         (= "TAG_Byte" type) (str "(byte) " value)
                         (= "TAG_Float" type) (str "(float) " value)
                         :default value)
                  generic (-> line (.split "entries of type ")
                              second get-java-type)
                  get-parent #(-> @stack peek %)
                  put-method (if type
                               (str "put" (-> type (.split "_") second)))
                  indent-str (apply str (repeat (count @stack) " "))
                  ]
              (cond
               (= type "TAG_Compound")
               (do
                 (swap! stack #(conj % {:type type :name name :numbered-name numbered-name :generic generic}))
                 (format "\n%sCompoundTag %s = new CompoundTag();\n" indent-str numbered-name))
               (= type "TAG_List")
               (do
                 (swap! stack #(conj % {:type type :name name :numbered-name numbered-name :generic generic}))
                 (format "\n%sListTag<%s> %s = new ListTag<%s>();\n" indent-str generic numbered-name generic))
               (comment-tags type)
               (str "//" line \newline)
               (leaf-tags type)
               (if (get-parent :generic)
                 ;parent must be a list.
                 (format "%s%s.add(new %s(\"\", %s));\n" indent-str (get-parent :numbered-name)
                         (get-parent :generic) value)
                 ;otherwise just normal
                 (format "%s%s.%s(\"%s\", %s);\n" indent-str (get-parent :numbered-name)
                         put-method name value))
               (.contains line "}")
               (let [
                     indent-str (apply str (repeat (dec (count @stack)) " "))
                     old-parent (peek @stack)
                     ]
                 (swap! stack pop)
                 (if (get-parent :generic)
                   (format "%s%s.add(%s);\n\n" indent-str
                           (get-parent :numbered-name) (old-parent :numbered-name))
                   (format "%s%s.put(\"%s\", %s);\n\n" indent-str
                           (get-parent :numbered-name) (old-parent :name) (old-parent :numbered-name))
                   )))))))

(def class-str
  "package com.chunkmapper;

public class Test {
public static void main(String[] args) {
%s
}}
")

(def f "/Users/matthewmolloy/workspace/chunkmapper2/src/com/chunkmapper/Test.java")
(spit f
      (format class-str (apply str code-lines)))

(.exec (Runtime/getRuntime) (str "open " f))
