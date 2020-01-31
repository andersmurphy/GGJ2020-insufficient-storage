(ns clj-my-first-game.core
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.paint Color]))

(def *state
  (atom {:gravity  10
         :friction 0.4}))

(defmulti event-handler :event/type)

(defn draw-entity [{:keys [color x y width height]} ^Canvas canvas]
  (doto (.getGraphicsContext2D canvas)
    (.clearRect x y width height)
    (.setFill color)
    (.fillRect x y width height)))

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
                                     :draw    (partial draw-entity {:color  Color/GREEN
                                                                    :x      50
                                                                    :y      50
                                                                    :height 50
                                                                    :width  50})}]}}}))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type root-view
                                    :state   state}))
   :opts {:fx.opt/map-event-handler event-handler}))

(comment (fx/mount-renderer *state renderer)
         )
