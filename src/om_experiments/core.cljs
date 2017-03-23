(ns om-experiments.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljsjs.quill]))

(enable-console-print!)

(println "Hello world!")

(defui HelloWorld
  Object
  (componentDidMount [this]
    (let [_ (println "Starting Quill")
          editor (js/Quill. "#description" #js {:debug "log" :theme "snow"})
          _ (println "Started Quill")]))

  (render [this]
    (dom/div nil
             (dom/div nil "Hello World!")
             (dom/div nil "Another hello")
             (dom/div #js {:id "description"}))))

(def hello (om/factory HelloWorld))

(js/ReactDOM.render (hello) (gdom/getElement "app"))
