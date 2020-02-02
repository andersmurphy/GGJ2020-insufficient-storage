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
       (filter #((set (remove :obstacle walls)) %))
       shuffle
       first))

(defn remove-midpoint [walls {x :x y :y} {x2 :x y2 :y}]
  (disj walls  {:x (/ (+ x x2) 2)
                :y (/ (+ y y2) 2)}))

(defn evolve [walls point backtracks board walls-over-time]
  (let [next-obstacle-fn (-> (filter :obstacle walls)
                             count
                             (drop [(fn [n] (> (int (* (:width board) (:height board) 0.95)) n))
                                    (fn [n] (> (int (* (:width board) (:height board) 0.85)) n))
                                    (fn [n] (> (int (* (:width board) (:height board) 0.75)) n))
                                    (fn [n] (> (int (* (:width board) (:height board) 0.65)) n))])
                             first)
        walls            (-> (if (and next-obstacle-fn (next-obstacle-fn (count walls)))
                               (conj walls (assoc point :obstacle true))
                               walls)
                             (disj point))
        neighbour        (first-valid-neighbour point walls board)]
    (cond
      neighbour          (recur (remove-midpoint walls point neighbour)
                                neighbour
                                (cons point backtracks)
                                board
                                (conj walls-over-time walls))
      (first backtracks) (recur walls
                                (first backtracks)
                                (rest backtracks)
                                board
                                walls-over-time)
      :else              walls-over-time)))

(defn generate-maze-points-overtime [{:keys [width height start-pos] :as board}]
  {:pre [(odd? width)
         (odd? height)]}
  (-> (create-walls board)
      (evolve start-pos [] board [])))

(comment (generate-maze-points-overtime {:height    21
                                         :width     21
                                         :start-pos {:x 1 :y 1}}))
