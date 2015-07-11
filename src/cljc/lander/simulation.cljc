(ns lander.simulation
  (:require [numerics.tableaus :as tableaus]
            [numerics.runge-kutta :as rk]
            [lander.terrain :as terrain]))

(def default-state
  { :game-state :live
   :gravity -9.81
   :state [0 0 190 0 0]
   :theta 0
   :thrust 0
   :landing-zones { :locations [-50 50] :width 10 }
   :lander { :width 6 :height 10 :max-landing-velocity 10 }})

(defn flatten-landing-zones [terrain lzs]
  (reduce
    (fn [t lz]
      (let [h (terrain/terrain-height lz t)
            hw (* 0.5 (lzs :width))]
        (terrain/make-flat ((juxt #(- % hw) #(+ % hw)) lz) h t)))
    terrain (lzs :locations)))

(defn reset-game []
  (let [t (terrain/gen-real {:roughness 100 :cells { 0 0 1 0 } } 8 -100 100)]
    (into default-state
          {:time (.getTime #?(:clj (java.util.Date.) :cljs (js/Date.)))
           :terrain (flatten-landing-zones t (default-state :landing-zones))})))

(defmulti sim :game-state)

(defmethod sim :live [{:keys [theta thrust time state gravity] :as s}]
  (let [t (.getTime #?(:clj (java.util.Date.) :cljs (js/Date.)))
        dt (* (- t time) 1E-3)
        dvx (fn [_] (-> theta (* Math/PI) (/ -180) Math/sin (* thrust)))
        dvy (fn [_] (+ gravity (-> theta (* Math/PI) (/ -180) Math/cos (* thrust))))
        new-states (rk/rk-step [#(% 3) #(% 4) dvx dvy] state dt tableaus/classic-fourth-order)]
    (into s { :state new-states :time t })))

(defmethod sim :default [state] state)
