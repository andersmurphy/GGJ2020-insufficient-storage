(ns clj-my-first-game.core
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.paint Color]))

(def *state
  (atom {:gravity  10
         :friction 0.4}))

(defmulti event-handler :event/type)

(defn root-view [{{:keys [gravity friction]} :state}]
  (let [width  400
        height 400]
    {:fx/type :stage
     :showing true
     :width   width
     :height  height
     :x       200
     :y       200
     :scene   {:fx/type :scene
               :root    {:fx/type  :h-box
                         :children [{:fx/type :canvas
                                     :height  height
                                     :width   width
                                     :draw    (fn [^Canvas canvas]
                                                (doto (.getGraphicsContext2D canvas)
                                                  (.clearRect 0 0 100 100)
                                                  (.setFill Color/GREEN)
                                                  (.fillRoundRect 0 0 100 200 100 100)
                                                  ))}]}}}))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type root-view
                                    :state   state}))
   :opts {:fx.opt/map-event-handler event-handler}))

(comment (fx/mount-renderer *state renderer)
         )
