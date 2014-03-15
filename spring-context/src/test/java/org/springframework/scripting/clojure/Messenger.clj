(ns Messenger)

(def my-calculator (get *spring-bindings* "thecalc"))
(def the-message (get *spring-bindings* "message"))

(reify org.springframework.scripting.Messenger
	(getMessage [this] (str the-message (.add my-calculator 4 9))))