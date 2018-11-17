(ns ledger.parser
  (:require
    [instaparse.core :as instaparse ]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(instaparse/defparser parser (io/resource "ledger2.bnf"))



(defn parse-ast
  ([str]
   (parse parser str))
  ([parser str]
   (instaparse/parse parser str)))

(defn ast->ledger [ast]
  (instaparse.transform/transform
    {:date             (fn [year month day]
                         (str year "-" month "-" day))
     :tag-name         (fn [tag]
                         (keyword "ledger" (str "tag." tag)))
     :tag-value        (fn [value]
                         value)
     :tag-typed-value  (fn [value]
                         ^::typed value)
     :real-account     (fn [& account]
                         ^::real-account (vec account))
     :virt-account     (fn [& account]
                         ^::virtual-account (vec account))
     ;:quantity (fn [& args]
     ;            (let [m (group-by first args)]
     ;              [:quantity {:ledger/amount ()}]
     ;              ))
     :amount           (fn [& args]
                         (let [m (group-by first args)
                               commodity (second (:commodity m))
                               ]
                           (-> {:ledger/amount (second (:number m))}
                               (cond-> (some? commodity)
                                       (merge {:ledger/commodity commodity
                                               :ledge})))))
     :bal-virt-account (fn [& account]
                         ^::balanced-virtual-account (vec account))
     ;:tx-posting      (fn [& args]
     ;                   (prn (group-by first args))
     ;                   (let [m (group-by first args)]
     ;                     [:tx-posting {:ledger/account ()}])
     ;                   )
     :tx               (fn [& items]
                         (let [m (group-by first items)]
                           (-> {:ledger/date           (second (first (get m :tx-date)))
                                :ledger/effective-date (second (first (get m :tx-effective-date)))
                                :ledger/flag           (second (first (get m :tx-flag)))
                                :ledger/code           (first (get m :tx-code))
                                :ledger/payee          (second (first (get m :tx-desc)))
                                :ledger/comments       (get m :comment)
                                :ledger/postings       (get m :tx-posting)}
                               (merge
                                 (->> (mapcat rest (get m :tags))
                                      (map (fn [t]
                                             [t nil]))
                                      (into {}))

                                 (->> (get m :tag-pair)
                                      (map (fn [[_ tag-name tag-value]]
                                             [tag-name tag-value]))
                                      (into {})))
                               (->> (remove (fn [[k v]]
                                              (and (not (str/starts-with? (name k) "tag."))
                                                   (nil? v))))
                                    (into {}))
                               (with-meta {::type :tx}))))} ast))

(defn parse-ledger [str]
  (-> (parse-ast str)
      (ast->ledger)))