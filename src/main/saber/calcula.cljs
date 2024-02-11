(ns saber.calcula
  (:require
   [instaparse.core :as insta]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [applied-science.js-interop :as j]
   ["mathjs" :as mathjs]))


(defmulti eval-expr
  (fn [expr _ctx _stack]
    (first expr)))


(defmethod eval-expr :number
  [[_ n] _ _]
  (let [n (s/replace n #"[,$]" "")]
    (js/parseFloat n)))


(defn apply-aggr
  [aggr coll]
  (case aggr
    "sum:"      (reduce + coll)
    "avg:"      (/ (reduce + coll) (count coll))
    "max:"      (apply max coll)
    "min:"      (apply min coll)
    "count:"    (count coll)
    "distinct:" (reduce + (distinct coll))
    "prev:"     (last coll)))


(defmethod eval-expr :aggr
  [[_label op] _ctx stack]
  (println "stack" stack)
  (apply-aggr op stack))


(defmethod eval-expr :assignment
  [[_label [_ sym] _= expr] ctx stack]
  (let [val (eval-expr (second expr) ctx stack)]
    (j/assoc! ctx sym val)
    val))


(defmethod eval-expr :id
  [[_ id] ctx _stack]
  (j/get ctx id))


(defmethod eval-expr :mathjs
  [[_ expr] ctx _stack]
  (mathjs/evaluate expr ctx))



(comment
  (eval-expr [:assignment [:id "x"] "=" [:expr [:number "$3"]]] (atom {}) []))


(defmethod eval-expr :binary-exp
  [[_ [_ a] [_ op] [_ b]] ctx stack]
  (let [a (eval-expr a ctx stack)
        b (eval-expr b ctx stack)]
    #_(mathjs/evaluate (str a op b))
    (case op
      "+" (+ a b)
      "-" (- a b)
      "*" (* a b)
      "x" (* a b)
      "/" (/ a b)
      "%" (mod a b)
      "^" (Math/pow a b)
      "**" (Math/pow a b)
      "//" (Math/pow a (/ 1 b)))))


(comment
  (eval-expr [:binary-exp [:expr [:number "$3"]] [:binary "^"] [:expr [:number "2"]]]))


  ;; doc = expr (<nl> expr)*
(def grammar "
doc = expr?
expr = number | assignment | aggr | binary-exp | id | mathjs

assignment = id '=' expr
aggr = 'sum:' | 'avg:' | 'max:' | 'min:' | 'count:' | 'distinct:' | 'prev:'

mathjs = <'mjs:'> #'.*'

binary-exp = expr binary expr
binary = '+' | '-' | '*' | '/' | '%' | '^' | '**' | 'x' | '//'

id = #'[a-zA-Z_][a-zA-Z0-9_-]*'
nl = #'\\n'
number = #'-?\\$?[0-9.,e]+'
")


(def parser (insta/parser grammar :auto-whitespace :standard))


(comment (parser "m: x = 5"))


(defn format-results
  [q+a]
  (let [width (apply max (map (comp count first) q+a))
        padding (+ width 10)
        ls (map (fn [[q a]] (when a (str (.padEnd q padding " ") " =>  " (.toLocaleString a))))
                q+a)]
    (str "```\n"
         (s/join "\n" ls)
         "\n```")))

(comment
  (format-results [["one" 1] ["two" 2] ["three" 3]]))



(defn process
  [doc]
  (let [ls             (s/split-lines doc)
        parsed         (mapv parser ls)
        grps           (partition-by #{[:doc]} parsed)
        ctx            (j/obj)
        processed (for [g grps]
                    (reduce (fn [acc [_ e]] (conj acc (when e (eval-expr (second e) ctx acc))))
                            []
                            g))
        q+a (mapv vector ls (flatten processed))
        _ (pprint q+a)]
    (format-results q+a)))


(comment
  (process "x = $3")
  (let [doc "
12
32
55 + 100
last-week = sum:

24
24
24
this-week = sum:

this-week - last-week
"]
    (process doc)))
