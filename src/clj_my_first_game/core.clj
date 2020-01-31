(ns clj-my-first-game.core
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.paint Color]))

(def *state
  (atom {:entities [{:color  Color/GREEN
                     :x      50
                     :y      50
                     :height 50
                     :width  50}
                    {:color  Color/GREEN
                     :x      100
                     :y      100
                     :height 50
                     :width  50}
                    {:color  Color/GREEN
                     :x      150
                     :y      150
                     :height 50
                     :width  50}]}))

(defmulti event-handler :event/type)

(defn draw-entity [^Canvas canvas {:keys [color x y width height] :as m}]
  (doto (.getGraphicsContext2D canvas)
    (.setFill color)
    (.fillRect x y width height)))

(defn draw-entities [canvas-width canvas-height entities ^Canvas canvas]
  (doto (.getGraphicsContext2D canvas)
    (.clearRect 0 0 canvas-width canvas-height))
  (run! (partial draw-entity canvas) entities))

(defn root-view [{{:keys [entities]} :state}]
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
                                     :draw    (partial draw-entities
                                                       canvas-width
                                                       canvas-height
                                                       entities)}]}}}))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type root-view
                                    :state   state}))
   :opts {:fx.opt/map-event-handler event-handler}))

(comment (fx/mount-renderer *state renderer))
