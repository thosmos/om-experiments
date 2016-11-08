(ns om-experiments.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(println "Hello world!")

(defui HelloWorld
  Object
  (componentDidMount [this]
    (println "Component Did Mount"))

  (render [this]
    (dom/div nil
             (dom/div nil "Hello World, Om.Next!"))))

(def hello (om/factory HelloWorld))

(js/ReactDOM.render (hello) (gdom/getElement "app"))
