(ns ufo-data.text
  (:require [clojure.string :as str]))

(def ^:dynamic *stop-list*
  #{"into" "yours" "again" "a" "were" "than" "all" "no" "against" "each" "during"
    "45" "once" "but" "nuforc" "both" "herself" "down" "be" "don" "or" "these"
    "most" "further" "there" "what" "having" "in" "through" "same" "with"
    "because" "themselves" "own" "where" "that" "such" "ours" "between" "its"
    "have" "they" "why" "i" "for" "was" "saw" "is" "from" "do" "it" "had" "am"
    "been" "just" "my" "any" "two" "our" "him" "only" "those" "an" "about" "we"
    "out" "whom" "off" "i'm" "how" "she" "other" "can" "here" "below" "the" "nor"
    "while" "as" "someone" "min" "wasn't" "his" "will" "note" "3" "at" "your"
    "see" "s" "should" "not" "t" "over" "some" "went" "too" "pd" "are" "sure"
    "would" "her" "more" "very" "now" "ourselves" "by" "of" "and" "itself"
    "doing" "himself" "me" "under" "myself" "few" "theirs" "does" "able" "their"
    "has" "when" "try" "to" "seen" "50" "up" "after" "them" "so" "he" "if"
    "which" "then" "yourselves" "above" "this" "min's" "until" "who" "hers" "you"
    "did" "being" "on" "yourself" "before"})

(defn replace-entities [s]
  (reduce #(apply str/replace %1 %2) s [["&apos;" "'"]
                                        ["&quot;" "\""]
                                        ["&amp;"  "&"]
                                        ["&ldquo;" "“"]
                                        ["&rdquo;" "”"]
                                        ["&lsquo;" "‘"]
                                        ["&rsquo;" "’"]]))


(defn normalize [s]
  (-> s
    str/trim
    str/lower-case
    replace-entities))

(defn tokenize
  "Tokenize the input."
  [input]
  (re-seq #"\p{L}+" input))

(defn get-bag [item]
  (set (remove *stop-list* (tokenize (normalize item)))))
