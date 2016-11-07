(ns om-experiments.core
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [compassus.core :as c]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [cljsjs.firebase]
            [firebase-cljs.core :as fb]
            [firebase-cljs.app]
            [firebase-cljs.auth :as fbauth]
            [firebase-cljs.database :as fbdb]
            )
  (:require-macros
    [cljs-log.core :refer [debug info warn severe]]))

(enable-console-print!)

(println "Hello world!")

;var config = {
;              apiKey: "AIzaSyBBIXjxuUd7zECpvnrwQ4EvJJ5G0pxjlDU",
;              authDomain: "take2dev.firebaseapp.com",
;              databaseURL: "https://take2dev.firebaseio.com",
;              storageBucket: "take2dev.appspot.com",
;              };


(def ->cljs #(js->clj % :keywordize-keys true))


(def fbconfig {:apiKey "AIzaSyBBIXjxuUd7zECpvnrwQ4EvJJ5G0pxjlDU"
               :authDomain "take2dev.firebaseapp.com"
               :databaseURL "https://take2dev.firebaseio.com"
               :storageBucket "take2dev.appspot.com"})

;(def fb js/firebase)
;(.. fb (initializeApp (clj->js fbconfig)))

(if (empty? (fb/get-apps))
  (fb/init fbconfig))

;(debug "FB APP" (->cljs (fb/get-app)))

(defn login []
  ;(fb/init fbconfig)
  (debug "FIREBASE VERSION" (fb/get-version) (fb/get-apps))

  ;(-> (.signInWithPopup (.auth js/firebase) (firebase.auth.FacebookAuthProvider.))
  ;    (.then (fn [result]
  ;             (info "FB LOGGED IN" (fb/->cljs result))))
  ;    (.catch (fn [error]
  ;              (warn "FB LOGIN ERROR" error))))

  (-> (.signInWithEmailAndPassword (.auth js/firebase) "thomas@thosmos.com" "123")
      (.then (fn [result error]
               (info "FB LOGIN" (js/console.log result) error))))

  )

;(.on (.ref (fb/get-db) "globals") "value" (fn [data]
;                                            (warn "firebase: " data)))

;(.. js/firebase database goOffline)

;(online (fb/get-db))




(defui One
  static om/IQuery
  (query [this]
    [:one/msg])
  Object
  (render [this]
    (let [{:keys [one/msg]} (om/props this)
          _ (debug "RENDER ONE")]
      (dom/div nil
               (dom/input #js {:type "button" :value "Login" :onClick #(login)})
               (dom/p nil msg)
               (dom/a #js {:href "two"
                           ;:onClick #(c/set-route! this :two)
                           }
                      "Two")
               (dom/a #js {:href "three"
                           ;:onClick #(c/set-route! this :two)
                           }
                      "Three")))))

(defui Two
  static om/IQuery
  (query [this]
    [:two/msg])
  Object
  (render [this]
    (let [{:keys [two/msg]} (om/props this)
          _ (debug "RENDER TWO")]
      (dom/div nil
               (dom/p nil msg)
               (dom/a #js {:href "one"
                           ;:onClick #(c/set-route! this :one)
                           }
                      "One")
               (dom/a #js {:href "three"
                           ;:onClick #(c/set-route! this :two)
                           }
                      "Three")))))

(defui Wrapper
  Object
  (render [this]
    (let [{:keys [owner factory props]} (om/props this)
          route (c/current-route this)
          _ (debug "RENDER WRAPPER props" props)]
      (dom/div nil
               (dom/p nil (str "Hello Wrapper! (route " route ")"))
               (factory props)))))

(def wrapper (om/factory Wrapper))

(def state (atom {:one {:one/msg "One"}
                  :two {:two/msg "Two"}
                  :three {:three/msg "THREE"}}))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

;; Basic default read function that covers many simple cases
(defmethod read :default
  [{:keys [state]} key _]

  (let [value (get @state key)]
    (debug "PARSE READ DEFAULT" key)
    {:value value}))

(defmethod read :one
  [{:keys [state]} key _]

  (let [value (get @state key)]
    (debug "PARSE READ DEFAULT" key)
    (let [;db (fb/get-db)
          ;globals (.. db (.ref "globals"))
          ])
    {:value value}))

(def parser (om/parser {:read read :mutate mutate}))

(def bidi-routes
  ["/" {"" :one
        "one" :one
        "two" :two
        "three" :three}])

(def routes
  {:one (c/index-route One)
   :two Two
   :three Two})

(declare app)

(def history
  (pushy/pushy #(c/set-route! app (:handler %))
               (partial bidi/match-route bidi-routes)))

(def app (c/application {:routes routes
                         :history {:setup #(pushy/start! history)
                                   :teardown #(pushy/stop! history)}
                         :wrapper wrapper
                         :reconciler-opts {:state state
                                           :parser parser
                                           :normalize true}}))

(c/mount! app (js/document.getElementById "app"))


