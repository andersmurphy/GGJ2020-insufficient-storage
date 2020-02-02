(defproject clj-my-first-game "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cljfx "1.6.2"]]
  :uberjar-name "uber.jar"
  :main clj-my-first-game.core
  :profiles {:uberjar
             {:aot        :all
              :injections [(javafx.application.Platform/exit)]}})
