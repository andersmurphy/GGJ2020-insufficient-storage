(ns clj-my-first-game.maze-gen)

(defn create-walls [{width :width height :height}]
  (set (for [x (range width)
             y (range height)]
         {:x x :y y})))

(defn neighbours-of [{x :x y :y}]
  [{:x (- x 2) :y y}
   {:x (+ x 2) :y y}
   {:x x :y (- y 2)}
   {:x x :y (+ y 2)}])

(defn position-inside-grid? [{x :x y :y} {width :width height :height}]

  (and (< -1 y height)
       (< -1 x width)))

(defn first-valid-neighbour [point walls board]
  (->> (neighbours-of point)
       (filter #(position-inside-grid? % board))
       (filter #(walls %))
       shuffle
       first))

(defn remove-midpoint [walls {x :x y :y} {x2 :x y2 :y}]
  (disj walls  {:x (/ (+ x x2) 2)
                :y (/ (+ y y2) 2)}))

(defn evolve [walls start board]
  (loop [walls      walls
         point      start
         backtracks []]
    (let [walls     (disj walls point)
          neighbour (first-valid-neighbour point walls board)]
      (cond
        neighbour          (recur (remove-midpoint walls point neighbour)
                                  neighbour
                                  (cons point backtracks))
        (first backtracks) (recur walls
                                  (first backtracks)
                                  (rest backtracks))
        :else              walls))))

(defn generate-maze-points [{:keys [width height start-pos] :as board}]
  {:pre [(odd? width)
         (odd? height)]}
  (-> (create-walls board)
      (evolve start-pos board)))
