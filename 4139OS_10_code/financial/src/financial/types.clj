(ns financial.types)

(defrecord NewsArticle [title pub-date text])

(defrecord StockData [date open high low close volume])
