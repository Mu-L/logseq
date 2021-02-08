(ns frontend.components.encryption
  (:require [rum.core :as rum]
            [frontend.encrypt :as e]
            [frontend.util :as util :refer-macros [profile]]
            [frontend.context.i18n :as i18n]
            [frontend.db.utils :as db-utils]
            [clojure.string :as string]
            [frontend.state :as state]
            [frontend.handler.metadata :as metadata-handler]))

(rum/defcs encryption-dialog-inner <
  (rum/local false ::reveal-secret-phrase?)
  [state repo-url close-fn]
  (let [reveal-secret-phrase? (get state ::reveal-secret-phrase?)
        secret-phrase (e/get-mnemonic repo-url)
        public-key (e/get-public-key repo-url)
        private-key (e/get-secret-key repo-url)]
    (rum/with-context [[t] i18n/*tongue-context*]
      [:div
       [:div.sm:flex.sm:items-start
        [:div.mt-3.text-center.sm:mt-0.sm:text-left
         [:h3#modal-headline.text-lg.leading-6.font-medium.text-gray-900
          "This graph is encrypted"]]]

       [:div.mt-1
        [:div.max-w-2xl.rounded-md.shadow-sm.sm:max-w-xl
         [:div.cursor-pointer.block.w-full.rounded-sm.p-2.text-gray-900
          {:on-click (fn []
                       (when (not @reveal-secret-phrase?)
                         (reset! reveal-secret-phrase? true)))}
          (if @reveal-secret-phrase?
            [:div
             [:div.font-medium.text-gray-900 "Secret Phrase:"]
             [:div secret-phrase]
             [:div.font-medium.text-gray-900 "Public Key:"]
             [:div public-key]
             [:div.font-medium.text-gray-900 "Private Key:"]
             [:div private-key]]
            "click to view the secret phrase")]]]

       [:div.mt-5.sm:mt-4.sm:flex.sm:flex-row-reverse
        [:span.mt-3.flex.w-full.rounded-md.shadow-sm.sm:mt-0.sm:w-auto
         [:button.inline-flex.justify-center.w-full.rounded-md.border.border-gray-300.px-4.py-2.bg-white.text-base.leading-6.font-medium.text-gray-700.shadow-sm.hover:text-gray-500.focus:outline-none.focus:border-blue-300.focus:shadow-outline-blue.transition.ease-in-out.duration-150.sm:text-sm.sm:leading-5
          {:type "button"
           :on-click close-fn}
          (t :close)]]]])))

(defn encryption-dialog
  [repo-url]
  (fn [close-fn]
    (encryption-dialog-inner repo-url close-fn)))

(rum/defcs input-password-inner <
  (rum/local "" ::password)
  [state repo-url close-fn]
  (rum/with-context [[t] i18n/*tongue-context*]
    (let [password (get state ::password)]
      [:div
       [:div.sm:flex.sm:items-start
        [:div.mt-3.text-center.sm:mt-0.sm:text-left
         [:h3#modal-headline.text-lg.leading-6.font-medium.text-gray-900
          "Enter a password"]]]

       [:input.form-input.block.w-full.sm:text-sm.sm:leading-5.my-2
        {:auto-focus true
         :style {:color "#000"}
         :on-change (fn [e]
                      (reset! password (util/evalue e)))}]

       [:div.mt-5.sm:mt-4.sm:flex.sm:flex-row-reverse
        [:span.flex.w-full.rounded-md.shadow-sm.sm:ml-3.sm:w-auto
         [:button.inline-flex.justify-center.w-full.rounded-md.border.border-transparent.px-4.py-2.bg-indigo-600.text-base.leading-6.font-medium.text-white.shadow-sm.hover:bg-indigo-500.focus:outline-none.focus:border-indigo-700.focus:shadow-outline-indigo.transition.ease-in-out.duration-150.sm:text-sm.sm:leading-5
          {:type "button"
           :on-click (fn []
                       (let [value @password]
                         (when-not (string/blank? value)
                           (when-let [mnemonic (e/generate-mnemonic-and-save! repo-url)]
                             (let [db-encrypted-secret (e/encrypt-with-passphrase value mnemonic)]
                               (metadata-handler/set-db-encrypted-secret! db-encrypted-secret)))
                           (close-fn true))))}
          "Submit"]]]])))

(defn input-password
  [repo-url close-fn]
  (fn [_close-fn]
    (input-password-inner repo-url close-fn)))

(rum/defcs encryption-setup-dialog-inner
  [state repo-url close-fn]
  (rum/with-context [[t] i18n/*tongue-context*]
    [:div
     [:div.sm:flex.sm:items-start
      [:div.mt-3.text-center.sm:mt-0.sm:text-left
       [:h3#modal-headline.text-lg.leading-6.font-medium.text-gray-900
        "Create encrypted graph?"]]]

     [:div.mt-5.sm:mt-4.sm:flex.sm:flex-row-reverse
      [:span.flex.w-full.rounded-md.shadow-sm.sm:ml-3.sm:w-auto
       [:button.inline-flex.justify-center.w-full.rounded-md.border.border-transparent.px-4.py-2.bg-indigo-600.text-base.leading-6.font-medium.text-white.shadow-sm.hover:bg-indigo-500.focus:outline-none.focus:border-indigo-700.focus:shadow-outline-indigo.transition.ease-in-out.duration-150.sm:text-sm.sm:leading-5
        {:type "button"
         :on-click (fn []
                     (state/set-modal! (input-password repo-url close-fn)))}
        (t :yes)]]
      [:span.mt-3.flex.w-full.rounded-md.shadow-sm.sm:mt-0.sm:w-auto
       [:button.inline-flex.justify-center.w-full.rounded-md.border.border-gray-300.px-4.py-2.bg-white.text-base.leading-6.font-medium.text-gray-700.shadow-sm.hover:text-gray-500.focus:outline-none.focus:border-blue-300.focus:shadow-outline-blue.transition.ease-in-out.duration-150.sm:text-sm.sm:leading-5
        {:type "button"
         :on-click (fn [] (close-fn false))}
        (t :no)]]]]))

(defn encryption-setup-dialog
  [repo-url close-fn]
  (fn [close-modal-fn]
    (let [close-fn (fn [encrypted?]
                     (close-fn encrypted?)
                     (close-modal-fn))]
      (encryption-setup-dialog-inner repo-url close-fn))))

(rum/defcs encryption-input-secret-inner <
  (rum/local "" ::secret)
  [state repo-url db-encrypted-secret close-fn]
  (rum/with-context [[t] i18n/*tongue-context*]
    (let [secret (get state ::secret)]
      [:div
       [:div.sm:flex.sm:items-start
        [:div.mt-3.text-center.sm:mt-0.sm:text-left
         [:h3#modal-headline.text-lg.leading-6.font-medium.text-gray-900
          (if db-encrypted-secret
            "Enter your password"
            "Enter your secret phrase")]]]

       [:input.form-input.block.w-full.sm:text-sm.sm:leading-5.my-2
        {:auto-focus true
         :style {:color "#000"}
         :on-change (fn [e]
                      (reset! secret (util/evalue e)))}]

       [:div.mt-5.sm:mt-4.sm:flex.sm:flex-row-reverse
        [:span.flex.w-full.rounded-md.shadow-sm.sm:ml-3.sm:w-auto
         [:button.inline-flex.justify-center.w-full.rounded-md.border.border-transparent.px-4.py-2.bg-indigo-600.text-base.leading-6.font-medium.text-white.shadow-sm.hover:bg-indigo-500.focus:outline-none.focus:border-indigo-700.focus:shadow-outline-indigo.transition.ease-in-out.duration-150.sm:text-sm.sm:leading-5
          {:type "button"
           :on-click (fn []
                       (let [value @secret]
                         (when-not (string/blank? value) ; TODO: length or other checks
                           (let [repo (state/get-current-repo)]
                             (if db-encrypted-secret
                               (e/save-mnemonic! repo (e/decrypt-with-passphrase value db-encrypted-secret))
                               (e/save-mnemonic! repo value))
                             (close-fn true)))))}
          "Submit"]]]])))

(defn encryption-input-secret-dialog
  [repo-url db-encrypted-secret close-fn]
  (fn [close-modal-fn]
    (let [close-fn (fn [encrypted?]
                     (close-fn encrypted?)
                     (close-modal-fn))]
      (encryption-input-secret-inner repo-url db-encrypted-secret close-fn))))
