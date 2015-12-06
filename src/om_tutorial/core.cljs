(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [datascript.core :as d])
  (:require-macros
   [cljs-log.core :refer [debug info warn severe]]))

(enable-console-print!)

(def init-data
  {:list/one [{:name "John" :points 0}
              {:name "Mary" :points 0}
              {:name "Bob" :points 0}]
   :list/two [{:name "Mary" :points 0 :age 27}
              {:name "Gwen" :points 0}
              {:name "Jeff" :points 0}]})

(def normed {:list/one
             [[:person/by-name "John"]
              [:person/by-name "Mary"]
              [:person/by-name "Bob"]],
             :list/two
             [[:person/by-name "Mary"]
              [:person/by-name "Gwen"]
              [:person/by-name "Jeff"]],
             :person/by-name
             {"John" {:name "John", :points 0},
              "Mary" {:name "Mary", :points 0, :age 27},
              "Bob" {:name "Bob", :points 0},
              "Gwen" {:name "Gwen", :points 0},
              "Jeff" {:name "Jeff", :points 0}},
             :om.next/tables #{:person/by-name}})

(defmulti read-a om/dispatch)

(defn get-people-a [state key]
  (let [st @state
        result (into [] (map #(get-in st %)) (get st key))
        _ (debug "READ-A" key result)]
    result))

(defmethod read-a :list/one
  [{:keys [state] :as env} key params]
  {:value (get-people-a state key)})

(defmethod read-a :list/two
  [{:keys [state] :as env} key params]
  {:value (get-people-a state key)})

(defmulti mutate-a om/dispatch)

(defmethod mutate-a 'points/increment
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
       [:person/by-name name :points]
       inc))})

(defmethod mutate-a 'points/decrement
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
       [:person/by-name name :points]
       #(let [n (dec %)] (if (neg? n) 0 n))))})

(defui Person-A
  static om/Ident
  (ident [this {:keys [name]}]
    [:person/by-name name])
  static om/IQuery
  (query [this]
    '[:name :points :age])
  Object
  (render [this]
    (debug "Render Person-A" (-> this om/props :name))
    (let [{:keys [points name] :as props} (om/props this)]
      (dom/li nil
        (dom/label nil (str name ", points: " points))
        (dom/button
          #js {:onClick
               (fn [e]
                 (om/transact! this
                   `[(points/increment ~props)]))}
          "+")
        (dom/button
          #js {:onClick
               (fn [e]
                 (om/transact! this
                   `[(points/decrement ~props)]))}
          "-")))))

(def person-a (om/factory Person-A {:keyfn :name}))

(defui ListView-A
  Object
  (render [this]
    (debug "Render ListView-A" (-> this om/path first))
    (let [list (om/props this)]
      (apply dom/ul nil
        (map person-a list)))))

(def list-view-a (om/factory ListView-A))

(defui RootView-A
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Person-A)]
      `[{:list/one ~subquery} {:list/two ~subquery}]))
  Object
  (render [this]
    (debug "Render RootView-A")
    (let [{:keys [list/one list/two]} (om/props this)]
      (apply dom/div nil
        [(dom/h2 nil "Map List A")
         (list-view-a one)
         (dom/h2 nil "Map List B")
         (list-view-a two)]))))

(def norm-data (om/tree->db RootView-A init-data true))

(def st (atom norm-data))

(def parser-a (om/parser {:read read-a :mutate mutate-a}))

(def reconciler-a
  (om/reconciler
    {:state st
     :parser parser-a}))

(om/add-root! reconciler-a
  RootView-A (gdom/getElement "app-a"))



;============================================================



(def conn
  (d/create-conn
    {:name {:db/unique :db.unique/identity}
     :db/ident {:db/unique :db.unique/identity}
     :list/one {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
     :list/two {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}}))

(d/transact! conn
  [{:name "John" :points 0}
   {:name "Mary" :points 0 :age 27}
   {:name "Bob" :points 0}
   {:name "Gwen" :points 0}
   {:name "Jeff" :points 0}
   {:db/ident :list/one
    :list/one [[:name "John"]
               [:name "Mary"]
               [:name "Bob"]]}
   {:db/ident :list/two
    :list/two [[:name "Mary"]
               [:name "Gwen"]
               [:name "Jeff"]]}])

(defmulti read-b om/dispatch)

(defn get-list
  "params:
     `conn` - a datascript connection
     `ident` - a :db/ident key for a list entity that uses the same key
      for its list of refs
     `subquery` - a pull subquery for its items' attributes
   returns: a vector of its items
   example:
      entity: {:db/ident :list/foo :list/foo [1 2]}
         use: (get-list :list/foo '[*])
     returns: [{:db/id 1 :a/start :no/where} {:db/id 2 :a/goal :now/here}]"
  [conn ident subquery]
  ; datascript supports datomic's dynamic `subquery`
  ; inside of a query's pull function (but the syntax
  ; is `?subquery` instead of `subquery`)
  ; Example: (d/q '[:find [(pull ?e ?subquery) ...] :in $ ?a ?subquery
  ; :where [?e ?a]] @state :list/one '[{:list/one [*]}])
  (let [subquery (or subquery '[*])
        ;_ (debug "get-people-b query" query)
        result (d/q '[:find [(pull ?e ?qry) ...]
                      :in $ ?a ?qry
                      :where [?e ?a]] @conn ident [{ident subquery}])
        result (-> result first ident)]
    result))

(defmethod read-b :list/one
  [{:keys [state query] :as env} key params]
  (let [r (get-list state key query)
        _ (debug "READ-B :list/one" r)]
    {:value r}))

(defmethod read-b :list/two
  [{:keys [state query] :as env} key params]
  (let [r (get-list state key query)
        _ (debug "READ-B :list/two" r)]
    {:value r}))

(defn dbfun! [db fun dbid attr]
  (let [q [:db/id attr]
        ent (d/pull db q dbid)
        old-val (attr ent)
        new-val (fun old-val)
        _ (debug "dbfun!" dbid attr old-val new-val)
        tx [{:db/id dbid attr new-val}]
        _ (debug "dbfun! tx" tx)]
    tx))

(defn txpoints! [state fun name]
  (d/transact! state [[:db.fn/call dbfun! fun [:name name] :points]]))

(defmulti mutate-b om/dispatch)

(defmethod mutate-b 'points/increment
  [{:keys [state]} _ {:keys [name]}]
  (let [_ (debug "MUTATE 'points/increment")]
    {:action (fn [] (txpoints! state inc name))}))

(defmethod mutate-b 'points/decrement
  [{:keys [state]} _ {:keys [name]}]
  (let [_ (debug "MUTATE 'points/decrement")
        fun #(let [n (dec %)] (if (neg? n) 0 n))]
    {:action (fn [] (txpoints! state fun name))}))

(defui Person-B
  static om/Ident
  (ident [this {:keys [name]}]
    [:name name])
  static om/IQuery
  (query [this]
    '[:name :points])
  Object
  (render [this]
    (debug "Render Person-B" (-> this om/props :name))
    (let [{:keys [points name] :as props} (om/props this)]
      (dom/li nil
        (dom/label nil (str name ", points: " points))
        (dom/button
          #js {:onClick
               (fn [e]
                 (om/transact! this
                   `[(points/increment ~props)]))}
          "+")
        (dom/button
          #js {:onClick
               (fn [e]
                 (om/transact! this
                   `[(points/decrement ~props)]))}
          "-")))))

(def person-b (om/factory Person-B {:keyfn :name}))

(defui ListView-B
  Object
  (render [this]
    (debug "Render ListView-B" (-> this om/path first))
    (let [list (om/props this)]
      (apply dom/ul nil
        (map person-b list)))))

(def list-view-b (om/factory ListView-B))

(defui RootView-B
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Person-B)
          _ (debug "RootView-B subquery" subquery)]
      `[{:list/one ~subquery} {:list/two ~subquery}]))
  Object
  (render [this]
    (debug "Render RootView-B")
    (let [{:keys [list/one list/two]} (om/props this)]
      (apply dom/div nil
        [(dom/h2 nil "Datascript List A")
         (list-view-b one)
         (dom/h2 nil "Datascript List B")
         (list-view-b two)]))))

(def parser-b (om/parser {:read read-b :mutate mutate-b}))

(def reconciler-b
  (om/reconciler
    {:state conn
     :parser parser-b}))

(om/add-root! reconciler-b
  RootView-B (gdom/getElement "app-b"))