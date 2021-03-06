(ns webchange.interpreter.renderer.scene.components.image.component
  (:require
    [webchange.interpreter.pixi :refer [Container Graphics Sprite WHITE]]
    [webchange.interpreter.renderer.scene.components.utils :as utils :refer [set-handler]]
    [webchange.interpreter.renderer.scene.filters.filters :refer [apply-filters]]
    [webchange.interpreter.renderer.scene.components.image.wrapper :refer [wrap]]
    [webchange.resources.manager :as resources]
    [webchange.logger :as logger]))

(def default-props {:x             {}
                    :y             {}
                    :width         {}
                    :height        {}
                    :scale         {:default {:x 1 :y 1}}
                    :name          {}
                    :on-click      {}
                    :ref           {}
                    :src           {:default nil}
                    :offset        {:default {:x 0 :y 0}}
                    :filters       {:default []}
                    :border-radius {}
                    :origin        {}
                    :max-width     {}
                    :max-height    {}})

(defn- apply-boundaries
  [container {:keys [max-width max-height]}]
  (when (and max-width (< max-width (.-width container)))
    (let [ratio (/ max-width (.-width container))]
      (set! (.-width container) (* ratio (.-width container)))
      (set! (.-height container) (* ratio (.-height container)))))
  (when (and max-height (< max-height (.-height container)))
    (let [ratio (/ max-height (.-height container))]
      (set! (.-width container) (* ratio (.-width container)))
      (set! (.-height container) (* ratio (.-height container))))))

(defn- apply-origin
  [container {{origin :type} :origin {x :x y :x} :offset}]
  (when (and origin (= 0 (int x)) (= 0 (int y)))
    (let [[h v] (clojure.string/split origin #"-")
          pivot (.-pivot container)]
      (case h
        "left" (set! (.-x pivot) 0)
        "center" (set! (.-x pivot) (/ (.-width container) 2))
        "right" (set! (.-x pivot) (.-width container))
        )
      (case v
        "top" (set! (.-y pivot) 0)
        "center" (set! (.-y pivot) (/ (.-height container) 2))
        "bottom" (set! (.-y pivot) (.-height container))))))

(defn- create-sprite
  [{:keys [src scale object-name width height]}]
  (let [resource (resources/get-resource src)]
    (when (and (-> resource nil?)
               (-> src nil? not)
               (-> src empty? not))
      (logger/warn (js/Error. (str "Resources for " src " were not loaded"))))
    (let [sprite (if (-> resource nil? not)
                   (Sprite. (.-texture resource))
                   (Sprite.))]
      (aset sprite "name" (str object-name "-sprite"))
      (utils/set-scale sprite scale)
      (utils/set-not-nil-value sprite "width" width)
      (utils/set-not-nil-value sprite "height" height)

      sprite)))

(defn- create-sprite-mask
  [{:keys [border-radius width height]}]
  (when (some? border-radius)
    (let [[lt rt rb lb] border-radius]
      (doto (Graphics.)
        (.beginFill 0x000000)
        (.moveTo lt 0)
        (.lineTo (- width rt) 0)                            ; top
        (.arc (- width rt) rt rt (* 0.75 Math/PI) 0)        ; top-right
        (.lineTo width (- height rb))                       ; right
        (.arc (- width rb) (- height rb) rb 0 (* 0.5 Math/PI)) ; bottom-right
        (.lineTo lb height)                                 ; bottom
        (.arc lb (- height lb) lb (* 0.5 Math/PI) Math/PI)  ; bottom-left
        (.lineTo 0 lt)                                      ; left
        (.arc lt lt lt Math/PI (* 0.75 Math/PI))            ; top-left
        (.endFill 0x000000)))))

(defn- create-sprite-container
  [{:keys [x y offset scale filters name]}]
  (let [position {:x (- x (* (:x offset) (:x scale)))
                  :y (- y (* (:y offset) (:y scale)))}]
    (doto (Container.)
      (aset "name" (str name "-sprite-container"))
      (utils/set-position position)
      (apply-filters filters))))

(def component-type "image")

(defn create
  "Create `image` component.

  Props params:
  :x - component x-position.
  :y - component y-position.
  :width - image width.
  :height - image height.
  :scale - image scale. Default: {:x 1 :y 1}.
  :name - component name that will be set to sprite and container with corresponding suffixes.
  :on-click - on click event handler.
  :ref - callback function that must be called with component wrapper.
  :src - image src. Default: nil.
  :offset - container position offset. Default: {:x 0 :y 0}.
  :filters - filters params to be applied to sprite. Default: [].
  :border-radius - make rounded corners. Radius in pixels. Default: 0.
  :origin - where image pivot will be set. Can be '(left|center|right)-(top|center|bottom)'. Default: 'left-top'.
  :max-width - max image width.
  :max-height - max image height."
  [{:keys [parent type on-click ref object-name] :as props}]
  (let [image (create-sprite props)
        image-mask (create-sprite-mask props)
        image-container (create-sprite-container props)
        wrapped-image (wrap type object-name image-container image)]

    (.addChild image-container image)
    (.addChild parent image-container)

    (when (some? image-mask)
      (.addChild image-container image-mask)
      (aset image "mask" image-mask))

    (when-not (nil? on-click) (set-handler image "click" on-click))
    (when-not (nil? ref) (ref wrapped-image))

    (apply-origin image-container props)
    (apply-boundaries image-container props)

    wrapped-image))
