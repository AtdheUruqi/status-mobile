(ns status-im2.contexts.chat.bottom-sheet-composer.effects
  (:require
    [status-im.async-storage.core :as async-storage]
    [react-native.core :as rn]
    [react-native.reanimated :as reanimated]
    [status-im2.contexts.chat.bottom-sheet-composer.constants :as constants]
    [status-im2.contexts.chat.bottom-sheet-composer.keyboard :as kb]
    [utils.number :as utils.number]))

(defn reenter-screen-effect
  [{:keys [text-value saved-cursor-position maximized?]}
   {:keys [content-height]}
   {:keys [input-content-height input-text input-maximized?]}]
  (when (and (empty? @text-value) (not= input-text nil))
    (reset! text-value input-text)
    (reset! content-height input-content-height)
    (reset! saved-cursor-position (count input-text)))
  (when input-maximized?
    (reset! maximized? true)))

(defn maximized-effect
  [{:keys [maximized?]}
   {:keys [height saved-height last-height]}
   {:keys [max-height]}
   {:keys [input-content-height]}]
  (when (or @maximized? (>= input-content-height max-height))
    (reanimated/animate height max-height)
    (reanimated/set-shared-value saved-height max-height)
    (reanimated/set-shared-value last-height max-height)))

(defn layout-effect
  [{:keys [lock-layout?]}]
  (when-not @lock-layout?
    (js/setTimeout #(reset! lock-layout? true) 500)))

(defn kb-default-height-effect
  [{:keys [kb-default-height]}]
  (when-not @kb-default-height
    (async-storage/get-item :kb-default-height
                            #(reset! kb-default-height (utils.number/parse-int % nil)))))

(defn background-effect
  [{:keys [maximized?]}
   {:keys [opacity background-y]}
   {:keys [max-height]}
   {:keys [input-content-height]}]
  (when (or @maximized? (>= input-content-height (* max-height constants/background-threshold)))
    (reanimated/set-shared-value background-y 0)
    (reanimated/animate opacity 1)))

(defn images-or-reply-effect
  [{:keys [container-opacity]}
   {:keys [replying? sending-images? input-ref]}
   images? reply?]
  (when (or images? reply?)
    (reanimated/animate container-opacity 1))
  (when (and (not @sending-images?) images? @input-ref)
    (.focus ^js @input-ref)
    (reset! sending-images? true))
  (when (and (not @replying?) reply? @input-ref)
    (.focus ^js @input-ref)
    (reset! replying? true))
  (when-not images?
    (reset! sending-images? false))
  (when-not reply?
    (reset! replying? false)))

(defn empty-effect
  [{:keys [text-value maximized? focused?]}
   {:keys [container-opacity]}
   images?
   reply?]
  (when (and (empty? @text-value) (not images?) (not reply?) (not @maximized?) (not @focused?))
    (reanimated/animate-delay container-opacity constants/empty-opacity 200)))

(defn component-will-unmount
  [{:keys [keyboard-show-listener keyboard-hide-listener keyboard-frame-listener]}]
  (.remove ^js @keyboard-show-listener)
  (.remove ^js @keyboard-hide-listener)
  (.remove ^js @keyboard-frame-listener))

(defn initialize
  [props state animations {:keys [max-height] :as dimensions} chat-input keyboard-height images? reply?]
  (rn/use-effect
   (fn []
     (maximized-effect state animations dimensions chat-input)
     (reenter-screen-effect state dimensions chat-input)
     (layout-effect state)
     (kb-default-height-effect state)
     (background-effect state animations dimensions chat-input)
     (images-or-reply-effect animations props images? reply?)
     (empty-effect state animations images? reply?)
     (kb/add-kb-listeners props state animations dimensions keyboard-height)
     #(component-will-unmount props))
   [max-height]))