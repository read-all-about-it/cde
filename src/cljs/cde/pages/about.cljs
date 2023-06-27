(ns cde.pages.about
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]
   [cde.components.nav :refer [page-header]]
   [markdown.core :refer [md->html]]))

(defn about-page []
  (r/with-let [page-text (rf/subscribe [:platform/about-page-text])]
    [:section.section>div.container>div.content
     [page-header "About"]
     (when @page-text
       [:div.block
        {:dangerouslySetInnerHTML {:__html (md->html @page-text)}}])]))

(defn team-page []
  (r/with-let [page-text (rf/subscribe [:platform/team-page-text])]
    [:section.section>div.container>div.content
     [page-header "Team"]
     (when @page-text
       [:div.block
        {:dangerouslySetInnerHTML {:__html (md->html @page-text)}}])]))

(defn faq-page []
  (r/with-let [page-text (rf/subscribe [:platform/faq-page-text])]
    [:section.section>div.container>div.content
     [page-header "FAQ"]
     (when @page-text
       [:div.block
        {:dangerouslySetInnerHTML {:__html (md->html @page-text)}}])]))