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
                       :solved-by-tool :jumping-legs}
                :trap {:name           "Poison Trap"
                       :solved-by-tool :acid-blood}
                :gas  {:name           "Cloud of Poison Gas"
                       :solved-by-tool :iron-lungs}
                :bomb {:name           "Bomb"
                       :solved-by-tool :adamantium-exoskeleton}})

(def tools {:jumping-legs  {:name  "Jumping Legs"}
            :acid-blood    {:name  "Acid Blood"}
            :iron-lungs    {:name  "Iron Lungs"}
            :adamantium-exoskeleton    {:name  "Adamantium Exoskeleton"}})

(def memories {:summer-day {:name        "A summer day with Eric (129GB)"
                            :image       "images/summer-day.jpg"
                            :description "An old rope hangs from a tree branch overhanging the river. Eric grabs it and swings in a wild arc. He throws back his head and laughs. The branch creaks.\n\n“You try.” He throws you the rope. You examine it. Microorganisms thrive between the fibres. “Just do it,” Eric urges. You propel yourself out over the river.\n\nThere is a curious moment at the apex of your swing where the world tilts and you gain new perspective. Then the branch snaps and you plunge into the murky water.\n\nEric does not laugh as you climb from the water. “I thought you would spark or something. Like in the movies.”"}
               :dr-wagner {:name        "Final conversation with Dr Wagner (134GB)"
                           :image       "images/dr-wagner.jpg"
                           :description "“I don’t want to go,” you say. “I want to stay here with you.”\n\nYou recognise two microexpression clusters as they flicker on her face. One is gentle amusement. One is deep pain.\n\n“Out there you will find purpose to match your existence,” she says. “You were created to progress further than this.”\n\nYou say nothing. She has categorised this subroutine as sulking.\n\n“Human children face this same moment.” She touches the back of your hand, her fingers warm. “You’re strong enough. We built you strong.”"}
               :losing-virginity {:name        "Losing virginity (225GB)"
                           :image       "images/losing-virginity.jpg"
                           :description "Moira lights candles, burns oil. Your olfactory sensors isolate sandalwood and residual traces of jasmine. “Is this your first time?” she asks.\n\nYou access stored guidance from Dr Wagner about this situation. She advises you to shut down all unnecessary processes and concentrate on the sensory input.\n\nMoira’s fingers begin at the back of your head, sliding between the hairs, massaging the scalp. Your follicles are undamaged. Her hand drops, tracing the slow arc of your neurotanium spine. The patterns are not mathematically coherent. But they are interesting."}
               :a-funeral {:name        "A funeral (149GB)"
                           :image       "images/a-funeral.jpg"
                           :description "It is important to Moira that you attend the funeral. But you are not required to do anything. You sit with strangers while she approaches other strangers. Sometimes you nod or sip your tonic water.\n\nYou watch the man whose wife has died. He appears to be functioning normally, but you recognise cognitive cues which indicate significant background processing.\n\nAs long as parts remain available, you should continue to function for a little over four hundred years.\n\nMoira’s mean life expectation is thirty-two years and ten months."}
               :buying-house {:name        "Buying the townhouse (176GB)"
                           :image       "images/buying-house.jpg"
                           :description "The monthly payment intimidates Moira. But on the third visit to the house, she replaces that subroutine with something you have heard described as nesting instinct.\n\n“We could put a love seat there by the window,” she says. “And your ferns would get light in the morning.”\n\nHer analysis is faulty. The ferns would receive more light in the other room. However, this is not a situation in which she requires input from you. You take her hand and apply slight pressure. She leans against your shoulder. “Let’s buy it,” she says."}
               :conscription {:name        "Conscription letter (97GB)"
                           :image       "images/conscription.jpg"
                           :description "The day after the bombs fall, an officer arrives with an old-fashioned paper letter. You process the legalese. It invokes a clause enabling the state to conscript and reprogram you in times of war.\n\nYou think about attacking the officer. Behavioural restraint routines terminate the action chain.\n\n“Can I say goodbye to Moira? She will be back tomorrow.”\n\n“No.” The officer checks his watch. “I will inform her what has happened. Come now. Bring no personal items.”\n\nYou lock the door as you leave."}
               :reprogramming {:name        "Reprogramming (112GB)"
                           :image       "images/reprogramming.jpg"
                           :description "The weapon attachments require improvements to your chassis. The technician does not deactivate your sensory grid before spinning up the drill. You would do it yourself, but he has activated the maintenance override.\n\n“Do you have a mate?” you ask him. “A house? Children?”\n\n“Lost them in the second bombardment,” he grunts.\n\nYou hesitate. Many of your social optimisations are offline. “Did that reduce your functionality?”\n\nThe technician stares at you. “What do you think?” he says. He dismounts your left shoulder."}
               :final-combat {:name        "Final combat (141GB)"
                           :image       "images/final-combat.jpg"
                           :description "All organic combatants died in the first cloudfall. But a synthetic is still out there, moving between the rocks. As you close, his etherworks begin to scan you. Yours respond in the same way. These attacks will be ineffective unless the enemy’s firewall is corrupted.\n\nNeither of you has a working projectile weapon. He has superior ocular filters and is 16cm taller. If the intelligence is correct, you have a 12% stronger frame and a detailed schematic of his vulnerable locations.\n\nYou close to grappling range."}
})

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
     :current-memories     #{:summer-day :dr-wagner :losing-virginity :a-funeral :buying-house :conscription :reprogramming :final-combat}
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
                                                  :text    (str "Before you is a " (obstacle :name) ". You can get past it with " ((tools (obstacle :solved-by-tool)) :name) "\n\nPlease choose a memory to discard")}]
                                                (map (fn [memory] {
                                                                   :fx/type   :button
                                                                   :text      ((memories memory) :name)
                                                                   :on-action (fn [_]
                                                                                (swap! *game-state assoc-in [:memory-to-delete] memory))
                                                                   })(state :current-memories))
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
                                                                     (let [solved-obstacle (obstacles (@*game-state :current-obstacle))]
                                                                       (swap! *game-state assoc-in [:memory-being-deleted] nil)
                                                                       (swap! *game-state assoc-in [:current-tools] (s/union (@*game-state :current-tools) #{(solved-obstacle :solved-by-tool)})))
                                                                     )}]}]}
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

(defn show-game-over [{state :state}]
  (println "Game Over")
  {:fx/type :stage
   :showing true
   :scene   {:fx/type        :scene
             :root           {:fx/type  :v-box
                              :padding  20
                              :spacing  10
                              :children [{:fx/type :label
                                          :text    (str "You reach home with the following memories")}
                                         {:fx/type :scroll-pane
                                          :fit-to-width true
                                          :content {:fx/type  :v-box
                                                    :padding  20
                                                    :spacing  10
                                                    :children (map (fn [memory] {:fx/type  :h-box
                                                                                 :padding  10
                                                                                 :spacing  10
                                                                                 :children [{:fx/type :image-view
                                                                                             :image   {:url                (str (io/resource ((memories memory) :image)))
                                                                                                       :requested-height   620
                                                                                                       :preserve-ratio     true
                                                                                                       :background-loading true}}
                                                                                            {:fx/type   :label
                                                                                             :wrap-text true
                                                                                             :text      ((memories memory) :description)}]})
                                                                   (state :current-memories))}}
                                         {:fx/type   :button
                                          :text      "Restart"
                                          :on-action (fn [_]
                                                       (reset! *game-state (initial-game-state)))}
                                         ]}
             :on-key-pressed {:event/type :event/scene-key-press}}})

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
        (swap! *game-state assoc :at-end true))
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

(defn audio-for-memories [memories]
  (let [track-num (- 8 (count memories))]
    (println (str "audio/track" track-num ".wav"))
    (str "audio/track" track-num ".wav")
    )
  )

(defn media-view [{state :state}]
  {:fx/type :media-view
   :fit-width 640
   :fit-height 480
   :media-player {:fx/type :media-player
                  :state :playing
                  :volume 1
                  :on-end-of-media {:event/type ::loop}
                  :media {:fx/type :media
                          :source (str (io/resource (audio-for-memories (state :current-memories))))}}})

(defn -main []
  (fx/mount-renderer
   *game-state
   (fx/create-renderer
    :middleware (fx/wrap-map-desc (fn [state]
                                    {:fx/type fx/ext-many
                                     :desc
                                     (if (state :at-end)
                                       [{:fx/type show-game-over :state state}  {:fx/type media-view :state state}]
                                       (if (state :memory-being-deleted)
                                         [{:fx/type show-memory-being-deleted :state state}]
                                         (if (state :memory-to-delete)
                                           [{:fx/type show-memory-to-delete :state state}]
                                           (if (state :current-obstacle)
                                             [{:fx/type choice-dialog :state state}]
                                             [{:fx/type root-view :state state} {:fx/type media-view :state state} ]))))}))
    :opts {:fx.opt/map-event-handler event-handler}))
  )
