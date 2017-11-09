(ns metabase.email-test
  "Various helper functions for testing email functionality."
  ;; TODO - Move to something like `metabase.test.util.email`?
  (:require [expectations :refer :all]
            [medley.core :as m]
            [metabase.email :as email]
            [metabase.test.data.users :as user]
            [metabase.test.util :as tu]))

(def inbox
  "Map of email addresses -> sequence of messages they've received."
  (atom {}))

(defn reset-inbox!
  "Clear all messages from `inbox`."
  []
  (reset! inbox {}))

(defn fake-inbox-email-fn
  "A function that can be used in place of `send-email!`.
   Put all messages into `inbox` instead of actually sending them."
  [_ email]
  (doseq [recipient (:to email)]
    (swap! inbox assoc recipient (-> (get @inbox recipient [])
                                     (conj email)))))

(defn do-with-fake-inbox
  "Impl for `with-fake-inbox` macro; prefer using that rather than calling this directly."
  [f]
  (with-redefs [metabase.email/send-email! fake-inbox-email-fn]
    (reset-inbox!)
    (tu/with-temporary-setting-values [email-smtp-host "fake_smtp_host"
                                       email-smtp-port "587"]
      (f))))

(defmacro with-fake-inbox
  "Clear `inbox`, bind `send-email!` to `fake-inbox-email-fn`, set temporary settings for `email-smtp-username`
   and `email-smtp-password` (which will cause `metabase.email/email-configured?` to return `true`, and execute BODY.

   Fetch the emails send by dereffing `inbox`.

     (with-fake-inbox
       (send-some-emails!)
       @inbox)"
  [& body]
  {:style/indent 0}
  `(do-with-fake-inbox (fn [] ~@body)))

(defn- create-email-body->regex-fn
  "Returns a function expecting the email body structure. It will apply the regexes in `regex-seq` over the body and
  return map of the stringified regex as the key and a boolean as the value. True if it returns results via `re-find`
  false otherwise."
  [regex-seq]
  (fn [message-body-seq]
    (let [{message-body :content} (first message-body-seq)]
      (zipmap (map str regex-seq)
              (map #(boolean (re-find % message-body)) regex-seq)))))

(defn regex-email-bodies
  "Takes a seq of regexes `regex-seq` that will will be applied to each email body in the fake inbox. The body will be
  replaced by a map with the stringified regex as it's key and a boolean indicated that the regex returned results."
  [& regexes]
  (let [email-body->regex-boolean (create-email-body->regex-fn regexes)]
    (m/map-vals (fn [emails-for-recipient]
                  (map #(update % :body email-body->regex-boolean) emails-for-recipient))
                @inbox)))

(defn email-to
  "Creates a default email map for `USER`, as would be returned by `with-fake-inbox`"
  [user-kwd & [email-map]]
  (let [{:keys [email]} (user/fetch-user user-kwd)]
    {email [(merge {:from "notifications@metabase.com",
                     :to [email]}
                    email-map)]}))

;; simple test of email sending capabilities
(expect
  [{:from    "notifications@metabase.com"
    :to      ["test@test.com"]
    :subject "101 Reasons to use Metabase"
    :body    [{:type    "text/html; charset=utf-8"
               :content "101. Metabase will make you a better person"}]}]
  (with-fake-inbox
    (email/send-message!
      :subject      "101 Reasons to use Metabase"
      :recipients   ["test@test.com"]
      :message-type :html
      :message      "101. Metabase will make you a better person")
    (@inbox "test@test.com")))
