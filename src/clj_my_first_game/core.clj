(ns clj-my-first-game.core
  (:gen-class)
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [clojure.set :as s]
            [clj-my-first-game.maze-gen :as maze-gen])
  (:import [javafx.scene Node]
           [javafx.animation SequentialTransition FadeTransition ScaleTransition TranslateTransition Interpolator]
           [javafx.util Duration]
           [javafx.scene.canvas Canvas]
           [javafx.scene.input KeyCode KeyEvent]
           [javafx.scene.paint Color]
           [javafx.scene.control DialogEvent Dialog ButtonType ButtonBar$ButtonData]))

(def tile-size 20)
;; board-width and height must be odd for maze gen to work
(def board-width 15)
(def board-height 15)
(def canvas-width  (* tile-size board-width))
(def canvas-height (* tile-size board-height))
(def entity-color (Color/web "#2E3440"))
(def obstacle-color Color/GREEN)
(def end-color Color/BLUE)

(defn points->entities [points]
  (map (fn [{:keys [obstacle end] :as point}]
         {:color (cond  obstacle obstacle-color
                        end      end-color
                        :default entity-color)
          :pos   point}) points))

(def obstacles {:pit  {:name           "Deep Pit"
                       :image          "DeepPitImage.png"
                       :solved-by-tool :jumping-legs}
                :trap {:name           "Deep Pit"
                       :image          "DeepPitImage.png"
                       :solved-by-tool :jumping-legs}
                :gas  {:name           "Deep Pit"
                       :image          "DeepPitImage.png"
                       :solved-by-tool :jumping-legs}
                :bomb {:name           "Deep Pit"
                       :image          "DeepPitImage.png"
                       :solved-by-tool :jumping-legs}})

(def tools {:jumping-legs {:name  "Jumping Legs"
                           :image "JumpingLegs.png"}
            :long-arms    {:name  "Long Arms"
                           :image "LongArms.pns"}})

(def memories {:summer-day {:name        "A summer day with Eric (129GB)"
                            :image       "images/summer-day.jpg"
                            :description "An old rope hangs from a tree branch overhanging the river. Eric grabs it and swings in a wild arc. He throws back his head and laughs. The branch creaks. “You try.” He throws you the rope. You examine it. Microorganisms thrive between the fibres. “Just do it,” Eric urges. You propel yourself out over the river. There is a curious moment at the apex of your swing where the world tilts and you gain new perspective. Then the branch snaps and you plunge into the murky water. Eric does not laugh as you climb from the water. “I thought you would spark or something. Like in the movies.”"}})

(defn collision? [{:keys [player maze-states-overtime]}]
  (some (partial = (:pos player))
        (first maze-states-overtime)))

(defn run-out-of-maze-states? [{:keys [maze-states-overtime]}]
  (empty? maze-states-overtime))

(defn maze-states-overtime []
  (maze-gen/generate-maze-points-overtime
   {:height    board-height
    :width     board-width
    :start-pos {:x 1 :y 1}}))

(defn initial-game-state []
  (let [maze-states-overtime (maze-states-overtime)]
    {:player               {:color Color/RED
                            :pos   {:x 1 :y 1}}
     :maze-states-overtime (drop 1 maze-states-overtime)
     :current-tools        #{}
     :current-memories     #{:summer-day}
     :obstacles            (->> (filter :obstacle (last maze-states-overtime))
                                (map (fn [obs-type-key obs]
                                       (assoc obs :type obs-type-key))
                                     (keys obstacles)))
     :end                  (-> (s/difference
                                (second (reverse maze-states-overtime))
                                (first (reverse maze-states-overtime)))
                               first
                               (assoc :end true))
     :at-end               nil
     :current-obstacle     nil
     :memory-to-delete     nil
     :memory-being-deleted nil}))

(def *game-state
  (atom (initial-game-state)
         :validator #(and (not (collision? %))
                          (not (run-out-of-maze-states? %)))))

(defn pass-obstacle [obstacle]
  (let [solving-tool (tools (obstacle :solved-by-tool))]
    {:fx/type :stage
     :showing true
     :scene   {:fx/type        :scene
               :root           {:fx/type  :v-box
                                :padding  20
                                :spacing  10
                                :children [{:fx/type :label
                                            :text    (str "You get past the " (obstacle :name) " with your " (solving-tool :name))}
                                           {:fx/type   :button
                                            :text      "Continue"
                                            :on-action (fn [_]
                                                         (swap! *game-state assoc-in [:current-obstacle] nil))}]}
               :on-key-pressed {:event/type :event/scene-key-press}}}))

(defn choose-memory-to-pass [obstacle state]
  {:fx/type :stage
   :showing true
   :scene   {:fx/type        :scene
             :root           {:fx/type  :v-box
                              :padding  20
                              :spacing  10
                              :children (concat [{:fx/type :label
                                                  :text    (str "Before you is a " (obstacle :name) ". You can get past it with " ((tools (obstacle :solved-by-tool)) :name) "\nPlease choose a memory to discard")}]
                                                (map (fn [memory] {
                                                                   :fx/type   :button
                                                                   :text      ((memories memory) :name)
                                                                   :on-action (fn [_]
                                                                                (swap! *game-state assoc-in [:memory-to-delete] memory))
                                                                   })(state :current-memories))
                                                ; [{:fx/type   :button
                                                ;   :text      "Cancel"
                                                ;   :on-action (fn [_]
                                                ;                (swap! *game-state assoc-in [:current-obstacle] nil))}]
                                                )}
             :on-key-pressed {:event/type :event/scene-key-press}}})


(defn- animate-delete-memory [^Node node]
  (doto (ScaleTransition. (Duration/seconds 1) node)
    (.setByX 3)
    (.setByY 3)
    (.setInterpolator Interpolator/EASE_BOTH)
    (.play))
  (doto (FadeTransition. (Duration/seconds 0.4) node)
    (.setFromValue 1.0)
    (.setToValue 0.4)
    (.setAutoReverse true)
    (.setCycleCount 12)
    (.play)))

(defn show-memory-being-deleted [{state :state}]
  (println "Show")
  (println (memories (state :memory-being-deleted)))
  (println (io/resource ((memories (state :memory-being-deleted)) :image)))
  (let [memory (memories (state :memory-being-deleted))]
    {:fx/type :stage
     :showing true
     :scene   {:fx/type        :scene
               :root           {:fx/type  :v-box
                                :padding  20
                                :spacing  10
                                :children [{:fx/type  :stack-pane
                                            :padding  20
                                            :children [{:fx/type  :h-box
                                                        :padding  10
                                                        :spacing  10
                                                        :children [{:fx/type :image-view
                                                                    :image   {:url                (str (io/resource (memory :image)))
                                                                              :requested-height   620
                                                                              :preserve-ratio     true
                                                                              :background-loading true}}
                                                                   {:fx/type   :label
                                                                    :wrap-text true
                                                                    :text      (memory :description)}]}
                                                       {:fx/type    fx/ext-on-instance-lifecycle
                                                        :on-created animate-delete-memory
                                                        :desc       {:fx/type :label
                                                                     :text    "DELETING!"
                                                                     :style   {:-fx-font [:bold 20 :sans-serif] :-fx-text-fill "red"}}}]}
                                           {:fx/type  :h-box
                                            :padding  10
                                            :spacing  10
                                            :children [{:fx/type   :button
                                                        :text      "Acknowledged"
                                                        :on-action (fn [_]
                                                                     (swap! *game-state assoc-in [:memory-being-deleted] nil))}]}]}
               :on-key-pressed {:event/type :event/scene-key-press}}}))

(defn show-memory-to-delete [{state :state}]
  (let [memory (memories (state :memory-to-delete))]
    {:fx/type :stage
     :showing true
     :scene   {:fx/type        :scene
               :root           {:fx/type  :v-box
                                :padding  20
                                :spacing  10
                                :children [{:fx/type  :h-box
                                            :padding  10
                                            :spacing  10
                                            :children [{:fx/type :image-view
                                                        :image   {:url                (str (io/resource (memory :image)))
                                                                  :requested-height   620
                                                                  :preserve-ratio     true
                                                                  :background-loading true}}
                                                       {:fx/type   :label
                                                        :wrap-text true
                                                        :text      (memory :description)}]}
                                           {:fx/type  :h-box
                                            :padding  10
                                            :spacing  10
                                            :children [{:fx/type   :button
                                                        :text      "Delete"
                                                        :on-action (fn [_]
                                                                     (swap! *game-state update-in [:current-memories] (fn [old] (s/difference old #{(state :memory-to-delete)})))
                                                                     (swap! *game-state assoc-in [:memory-being-deleted] (state :memory-to-delete))
                                                                     (swap! *game-state assoc-in [:memory-to-delete] nil))}
                                                       {:fx/type   :button
                                                        :text      "Cancel"
                                                        :on-action (fn [_]
                                                                     (swap! *game-state assoc-in [:memory-to-delete] nil))}]}]}
               :on-key-pressed {:event/type :event/scene-key-press}}}))

(defn choice-dialog [{state :state}]
  (let [obstacle (if (state :current-obstacle)
                   (obstacles (state :current-obstacle))
                   {:name           "Obstacle"
                    :image          ""
                    :solved-by-tool nil})]
    (if (contains? (state :current-tools) (obstacle :solved-by-tool))
      (pass-obstacle obstacle)
      (choose-memory-to-pass obstacle state)
)))

(defn draw-entity [^Canvas canvas {color       :color
                                   {x :x y :y} :pos}]
  (doto (.getGraphicsContext2D canvas)
    (.setFill color)
    (.fillRect (* tile-size x) (* tile-size y) tile-size tile-size)))

(defn draw-entities [entities ^Canvas canvas]
  (doto (.getGraphicsContext2D canvas)
    (.clearRect 0 0 canvas-width canvas-height))
  (run! (partial draw-entity canvas) entities))

(defn root-view [{{:keys [maze-states-overtime player end]} :state}]
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
                                                            (conj
                                                             (points->entities
                                                              (concat
                                                               [end]
                                                               (first maze-states-overtime)))
                                                             player))}]}
             :on-key-pressed {:event/type :event/scene-key-press}}})

(defn update-game-state! [f & args]
  (try
    (let [{{player-pos :pos} :player
           obstacles         :obstacles
           end               :end} (apply swap! *game-state f args)]
      (when-let [{obs-type :type} (first (filter (fn [obstacle]
                                                   (= player-pos
                                                      {:x (:x obstacle)
                                                       :y (:y obstacle)}))
                                                 obstacles))]
        (swap! *game-state assoc :current-obstacle obs-type))
      (when (= {:x (:x end) :y (:y end)} player-pos)
        (swap! *game-state assoc :at-end true)

        ;; Resets the game you probably want to call this in your final dialog
        ;; I put it here as an example
        (reset! *game-state (initial-game-state)))
      (swap! *game-state update :maze-states-overtime (fn [x] (drop 1 x))))
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
        action   (key->action key-code)]
    (when action (action e))))

(defn -main []
  (fx/mount-renderer
   *game-state
   (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type fx/ext-many
                                    :desc
                                    (if (state :memory-being-deleted)
                                      [{:fx/type show-memory-being-deleted :state state}]
                                      (if (state :memory-to-delete)
                                        [{:fx/type show-memory-to-delete :state state}]
                                        (if (state :current-obstacle)
                                          [{:fx/type choice-dialog :state state}]
                                          [{:fx/type root-view :state state}])))}))
   :opts {:fx.opt/map-event-handler event-handler})))
