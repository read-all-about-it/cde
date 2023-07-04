(ns cde.views.home
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [markdown.core :refer [md->html]]
   [cde.components.nav :refer [page-header]]
   [cde.utils :refer [pretty-number]]))

;; THE 'HOME' PAGE ('/')
;; The introduction/root page of the application, with a brief description of
;; the project, highlights, a link to sign-up, and a link to explore via the
;; search page.

(defn- big-title
  "A big title for the home page."
  []
  [:div.block.has-text-centered
   [:h3.subtitle.is-3 "Welcome To"]
   [:h1.title.is-1 [:em "To Be Continued"]]
   [:h2.subtitle.is-3 "The Australian Newspaper Fiction Database"]])

(def explanation
  "In the 19th and 20th centuries, Australian newspapers contained a huge array of content. In fact, newspapers and periodicals were the main source of fiction for Australian readers for much of that time. 

Until recently, thousands of works of fiction were locked away in archives and little was known about the stories published in Australia’s newspapers. Now for the first time, we can explore this fiction through *To Be Continued: The Australian Newspaper Fiction Database*, an interactive database of more than 40,000 works of fiction sourced from the National Library of Australia’s Trove collection of digitised newspapers. 

[Find out more](#/about) about the *To Be Continued: The Australian Newspaper Fiction Database*, [who built it](#/team) and [how you can get involved](#/faq).")

(defn- home-page-explainer
  "A block of paragraphs explaining the To Be Continued platform."
  [text-block]
  [:div.block.has-text-centered
   {:dangerouslySetInnerHTML {:__html (md->html text-block)}}])


(defn- acknowledgement-of-country
  "A block of text acknowledging the true owners of the land on which this project was developed."
  []
  [:footer.footer
   [:div.content.has-text-centered
    [:p "The To Be Continued team would like to acknowledge the Ngunnawal and Ngambri people on whose traditional lands the National Library of Australia and the Australian National University are located, as well as the many peoples on whose lands we work. These include the Yuggera and Bundjalung peoples on whose traditional lands Griffith University is located, and the Djabugay, Yirrganydji and Gimuy Yidinji peoples, traditional owners of the lands on which James Cook University’s Cairns campus is based."]]])

(defn- record-count
  "A simple component to display the number of records in the database."
  []
  (r/with-let [newspaper-count (rf/subscribe [:platform/newspaper-count])
               title-count (rf/subscribe [:platform/title-count])
               chapter-count (rf/subscribe [:platform/chapter-count])
               author-count (rf/subscribe [:platform/author-count])]
    (fn []
      (when (and (some? @newspaper-count) (some? @title-count) (some? @chapter-count) (some? @author-count))
        [:nav.level.is-mobile
         [:div.level-item.has-text-centered
          [:div
           [:p.heading "Newspapers"]
           [:p.title (pretty-number @newspaper-count)]]]
         [:div.level-item.has-text-centered
          [:div
           [:p.heading "Titles"]
           [:p.title (pretty-number @title-count)]]]
         [:div.level-item.has-text-centered
          [:div
           [:p.heading "Chapters"]
           [:p.title (pretty-number @chapter-count)]]]
         [:div.level-item.has-text-centered
          [:div
           [:p.heading "Authors"]
           [:p.title (pretty-number @author-count)]]]]))))



(defn home-page []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])]
    (fn []
      [:section.section>div.container
       [:div.content
        [big-title]
        [record-count]
        [home-page-explainer explanation]]
       [acknowledgement-of-country]])))