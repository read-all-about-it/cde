(ns cde.pages.home
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [markdown.core :refer [md->html]]
   [cde.components.login :as login]))

;; THE 'HOME' PAGE ('/')
;; The introduction/root page of the application, with a brief description of
;; the project, highlights, a link to sign-up, and a link to explore via the
;; search page.

(defn home-page []
   (r/with-let [landing-page-text (rf/subscribe [:landing-page])
                newspaper-count (rf/subscribe [:platform/newspaper-count])
                title-count (rf/subscribe [:platform/title-count])
                chapter-count (rf/subscribe [:platform/chapter-count])
                author-count (rf/subscribe [:platform/author-count])
                logged-in? (rf/subscribe [:auth/logged-in?])]
     (fn []
       [:section.section>div.container>div.content
        [:h1 {:style {:text-align "center"}}
         "Welcome to the 'To Be Continued' project!"]
        (when @landing-page-text
          [:div {:dangerouslySetInnerHTML {:__html (md->html @landing-page-text)}}])
        [:br]
        (when-not @logged-in?
          [:div.container {:style {:text-align "center"}}
           (when (and (some? @newspaper-count) (some? @title-count) (some? @chapter-count) (some? @author-count))
             [:p (str "Right now, there are " @newspaper-count " newspapers, " @author-count " authors, " @title-count " titles, and " @chapter-count " chapters in the database. Help us add more!")])
           [login/register-button]
           ])])))