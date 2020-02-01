(ns clj-my-first-game.core
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.input KeyCode KeyEvent]
           [javafx.scene.paint Color]
           [javafx.scene.control DialogEvent Dialog ButtonType ButtonBar$ButtonData]))

(def tile-size 20)
(def board-width 20)
(def board-height 20)
(def canvas-width  (* tile-size board-width))
(def canvas-height (* tile-size board-height))

(def obstacles {:pit {:name            "Deep Pit"
                      :image           "DeepPitImage.png"
                      :solved-by-tools #{:jumping-legs}}})
(def tools {:jumping-legs {:name         "Jumping Legs"
                           :image        "JumpingLegs.png"
                           :loses-memory :bicycle}})
(def memories {:bicycle {:name  "Bicycle"
                         :image "BicycleImage.png"}})

(defn no-collision? [{:keys [player entities]}]
  (not (->> (map :pos entities)
            (some (partial = (:pos player))))))

(defn boarder-entites []
  (->> (concat (map (fn [x][x 0]) (range board-width))
               (map (fn [x][x (dec board-height )]) (range board-width))
               (map (fn [y][0 y]) (range board-height))
               (map (fn [y][(dec board-width) y]) (range board-height)))
       (map (fn [[x y]] {:color Color/GREEN
                         :pos   {:x x :y y}}))))

(def *game-state
  (atom {:player           {:color Color/RED
                            :pos   {:x 1 :y 1}}
         :entities         (concat (boarder-entites))
         :current-tools    []
         :current-memories [:bicycle]
         :current-obstacle nil}
        :validator no-collision?))

(defn choice-dialog [_]
  (let [obstacle (if (@*game-state :current-obstacle)
                   ((obstacles (@*game-state :current-obstacle) :name))
                   {:name            "Obstacle"
                    :image           ""
                    :solved-by-tools #{}})]
    {:fx/type :stage
     :showing true
     :scene   {:fx/type :scene
               :root    {:fx/type  :v-box
                         :padding  20
                         :spacing  10
                         :children [{:fx/type :label
                                     :text    "Please choose a memory to discard"}
                                    {:fx/type   :button
                                     :text      (obstacle :name)
                                     :on-action (fn [_]
                                                  (swap! *game-state assoc-in [:current-obstacle] nil))}]}}}))

(defn draw-entity [^Canvas canvas {color       :color
                                   {x :x y :y} :pos}]
  (doto (.getGraphicsContext2D canvas)
    (.setFill color)
    (.fillRect (* tile-size x) (* tile-size y) tile-size tile-size)))

(defn draw-entities [entities ^Canvas canvas]
  (doto (.getGraphicsContext2D canvas)
    (.clearRect 0 0 canvas-width canvas-height))
  (run! (partial draw-entity canvas) entities))

(defn root-view [{{:keys [entities player]} :state}]
  {:fx/type :stage
   :showing true
   :width   canvas-width
   :height  (+ canvas-height tile-size)
   :x       200
   :y       200
   :scene   {:fx/type        :scene
             :root           {:fx/type  :h-box
                              :children [{:fx/type :canvas
                                          :height  canvas-height
                                          :width   canvas-width
                                          :draw    (partial draw-entities
                                                            (conj entities player))}]}
             :on-key-pressed {:event/type :event/scene-key-press}}})

(defn update-game-state! [f & args]
  (try
    (apply swap! *game-state f args)
    (catch Exception IllegalStateException)))

(def key->action
  {"W" (fn [_] (update-game-state! update-in [:player :pos :y] dec))
   "S" (fn [_] (update-game-state! update-in [:player :pos :y] inc))
   "A" (fn [_] (update-game-state! update-in [:player :pos :x] dec))
   "D" (fn [_] (update-game-state! update-in [:player :pos :x] inc))
   "X" (fn [_] (swap! *game-state assoc-in [:current-obstacle] :pit))
   "Y" (fn [_] (swap! *game-state assoc-in [:current-obstacle] nil))})

(defmulti event-handler :event/type)
(defmethod event-handler :event/scene-key-press [e]
  (let [key-code (str (.getCode ^KeyEvent (:fx/event e)))
        action   (key->action key-code )]
    (when action (action e))))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type fx/ext-many
                                    :desc    (if (state :current-obstacle)
                                               [{:fx/type choice-dialog}]
                                               [{:fx/type root-view :state state}])}))
   :opts {:fx.opt/map-event-handler event-handler}))

(fx/mount-renderer *game-state renderer)
