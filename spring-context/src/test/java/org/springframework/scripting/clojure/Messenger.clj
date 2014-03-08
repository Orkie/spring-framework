(ns Messenger)

(defn setMessage [val] (def message-val val))

(reify org.springframework.scripting.Messenger
	(getMessage [this] message-val))