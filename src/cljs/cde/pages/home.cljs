(ns cde.pages.home
  (:require 
   [re-frame.core :as rf] 
   [cde.events]
   [markdown.core :refer [md->html]]))

;; THE 'HOME' PAGE ('/')
;; The introduction/root page of the application, with a brief description of
;; the project, highlights, a link to sign-up, and a link to explore via the
;; search page.

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [home-page-dummy-text @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html home-page-dummy-text)}}])])