(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(println "Hello world!")

(def init-data
  (atom {:current-user {:email "bob.smith@gmail.com"}
    :things [{:id 0
              :title "A thing"
              :items [{:id 0 :title "Foo"}
                            {:id 1 :title "Bar"}
                            {:id 2 :title "Baz"}]}]
    :activist {:id 0
               :name "Activisto"
               :item [:item 0]}}))

(defmulti read om/dispatch)

(defmethod read :things
  [{:keys [query state]} k _]
  (let [st @state]
    {:value (om/db->tree query (get st k) st)}))

(defmethod read :thing
  [{:keys [query state ast]} k _]
  (let [st @state
        value (get-in st (:key ast))
        _ (println "read :thing value" value)]
    {:value value}))

(defmethod read :item
  [{:keys [query state ast]} k _]
  (let [st @state
        value (get-in st (:key ast))
        _ (println "read :item value" value)]
    {:value value}))

(defmethod read :activist
  [{:keys [query state]} k _]
  (let [st @state]
    {:value (om/db->tree query (get st k) st)}))

(defmethod read :default
  [{:keys [query state]} k _]
  (let [st @state
        _ (println "READ :default" k query)]
    {:value (om/db->tree query (get st k) st)}))

(defui Item
  static om/Ident
  (ident [_ props]
    [:item (:id props)])
  static om/IQuery
  (query [_]
    [:id :title])
  Object
  (render [this]
    (let [{:keys [title]} (om/props this)]
      (dom/li nil
        (str "Item: " title)))))

(def item (om/factory Item))

(defui Thing
  static om/Ident
  (ident [_ props]
    [:thing (:id props)])
  static om/IQuery
  (query [_]
    [:id :title {:items (om/get-query Item)}])
  Object
  (render [this]
    (let [{:keys [title]} (om/props this)]
      (dom/div nil
        (dom/div nil (str "Thing: " title))
        (dom/ul nil
          (map item (-> this om/props :items)))))))

(def thing (om/factory Thing))

(defui Activist
  static om/IQuery
  (query [this]
    [:id :name :item])
  Object
  (render [this]
    (let [{:keys [name item]} (om/props this)
          _ (println "Item: " item)]
      (dom/div nil
        (dom/div nil (str "Activist: " name))
        (dom/div nil (str "Item: " item))))))

(def activ (om/factory Activist))

(defui App
  static om/IQuery
  (query [this]
    [{:things (om/get-query Thing)}
     {:activist (om/get-query Activist)}])
  Object
  (render [this]
    (let [{:keys [things activist]} (om/props this)]
      (dom/div nil
        (dom/div nil (thing (first things)))
        (dom/div nil (activ activist))))))

(def parser (om/parser {:read read}))

(def reconciler
  (om/reconciler
    {:state init-data
     :parser parser
     :normalize true
     :pathopt true}))

(om/add-root! reconciler App (gdom/getElement "app"))