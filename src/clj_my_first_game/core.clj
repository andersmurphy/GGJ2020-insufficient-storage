(ns clj-my-first-game.core
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.paint Color]))

(def *state
  (atom {:player {:color  Color/GREEN
                  :x      100
                  :y      100
                  :height 50
                  :width  50}}))

(defmulti event-handler :event/type)

(defn draw-entity [canvas-width canvas-height {:keys [color x y width height]} ^Canvas canvas]
  (doto (.getGraphicsContext2D canvas)
    (.clearRect 0 0 canvas-width canvas-height)
    (.setFill color)
    (.fillRect x y width height)))

(defn root-view [{{:keys [player]} :state}]
  (let [canvas-width  400
        canvas-height 400]
    {:fx/type :stage
     :showing true
     :width   canvas-width
     :height  canvas-height
     :x       200
     :y       200
     :scene   {:fx/type :scene
               :root    {:fx/type  :h-box
                         :children [{:fx/type :canvas
                                     :height  canvas-height
                                     :width   canvas-width
                                     :draw    (partial draw-entity
                                                       canvas-width
                                                       canvas-height
                                                       player)}]}}}))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type root-view
                                    :state   state}))
   :opts {:fx.opt/map-event-handler event-handler}))

(comment (fx/mount-renderer *state renderer)
         (swap! *state update-in [:player :x] - 50)
         )
