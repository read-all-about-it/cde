(ns cde.views.about
  "Static content pages: About, Team, and FAQ.

  These views render markdown content fetched from the backend,
  providing information about the To Be Continued project, the
  team behind it, and frequently asked questions.

  Content is loaded via platform subscriptions and rendered as HTML."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]
   [cde.components.nav :refer [page-header]]
   [markdown.core :refer [md->html]]))

(defn about-page
  "Renders the About page with project information.

  Displays markdown content from the `:platform/about-page-text` subscription,
  describing the To Be Continued platform and its purpose."
  []
  (r/with-let [page-text (rf/subscribe [:platform/about-page-text])]
    (fn []
      [:section.section>div.container>div.content
       [page-header "About"]
       (when @page-text
         [:div.block
          {:dangerouslySetInnerHTML {:__html (md->html @page-text)}}])])))

(defn team-page
  "Renders the Team page with contributor information.

  Displays markdown content from the `:platform/team-page-text` subscription,
  listing the people behind the To Be Continued project."
  []
  (r/with-let [page-text (rf/subscribe [:platform/team-page-text])]
    (fn []
      [:section.section>div.container>div.content
       [page-header "Team"]
       (when @page-text
         [:div.block
          {:dangerouslySetInnerHTML {:__html (md->html @page-text)}}])])))

(defn faq-page
  "Renders the FAQ page with common questions and answers.

  Displays markdown content from the `:platform/faq-page-text` subscription."
  []
  (r/with-let [page-text (rf/subscribe [:platform/faq-page-text])]
    (fn []
      [:section.section>div.container>div.content
       [page-header "Frequently Asked Questions"]
       (when @page-text
         [:div.block
          {:dangerouslySetInnerHTML {:__html (md->html @page-text)}}])])))
