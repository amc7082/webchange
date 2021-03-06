(ns webchange.editor-v2.translator.state.window
  (:require
    [re-frame.core :as re-frame]
    [webchange.editor-v2.translator.state.db :refer [path-to-db]]
    [webchange.editor-v2.translator.translator-form.state.form :as translator-form]))

;; Subs

(re-frame/reg-sub
  ::modal-state
  (fn [db]
    (-> db
        (get-in (path-to-db [:translator-modal-state]))
        boolean)))

;; Events

(re-frame/reg-event-fx
  ::open
  (fn [{:keys [db]} [_]]
    {:db         (assoc-in db (path-to-db [:translator-modal-state]) true)
     :dispatch [::translator-form/init-state]}))

(re-frame/reg-event-fx
  ::close
  (fn [{:keys [db]} [_]]
    {:db         (assoc-in db (path-to-db [:translator-modal-state]) false)
     :dispatch-n (list [::translator-form/reset-state])}))
