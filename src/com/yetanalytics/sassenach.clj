(ns com.yetanalytics.sassenach
  (:require [sass4clj.core :as sass]
            [clojure.java.io :as io]
            [hawk.core :as hawk])
  (:import [java.io File]
           [java.lang Thread Runtime]))

(set! *warn-on-reflection* true)

(defn compile!
  "Compile SASS file(s) to css. Input path is the root Sass file, output path is
   the compiled CSS file. No arg version attempts to read config from sass.edn
   in project root."
  [input-path output-path & {:as opts}]
  (sass/sass-compile-to-file
   input-path
   output-path
   opts))

(defn input->watch-paths
  "Given an input SCSS file, infer the :paths key for Hawk"
  [input-path]
  (let [^File f (io/file input-path)]
    (if (.exists f)
      [(.. f
           getAbsoluteFile
           getParentFile
           getAbsolutePath)]
      (throw (ex-info "Input file not found!"
                      {:type ::input-not-found
                       :input-path (.getAbsolutePath f)})))))

(defmacro time-ms
  [expr]
  `(let [start# (. System (nanoTime))]
     ~expr
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))

(defn watch-compile
  "Compiles input-path to output-path.
   Returns a function that stops the watch."
  [input-path
   output-path
   & add-watch-paths]
  (println "Starting SASS autocompiler...")
  (println "Initial build...")
  (try
    ;; If the input file isn't there, getting the watch-paths will throw
    (let [comp-fn (fn []
                    (printf "\nSuccessful build in %s ms\n"
                            (time-ms
                             (compile! input-path
                                       output-path
                                       :source-map true
                                       :source-paths (into []
                                                           add-watch-paths))))
                    (flush))
          watch-paths (input->watch-paths input-path)]
      (comp-fn)
      (let [watcher
            (hawk/watch!
             [{:paths (into watch-paths add-watch-paths)
               :filter hawk/file?
               :handler (fn [ctx {:keys [kind ^File file]}]
                          (printf "\nDetected change:\n  %s\nRecompiling...\n"
                                  (.getAbsolutePath file))
                          (flush)
                          (try (comp-fn)
                               (catch clojure.lang.ExceptionInfo exi
                                 (let [{ex-type :type
                                        :keys [formatted]
                                        :as exd} (ex-data exi)]
                                   (if (= ::sass/error ex-type)
                                     (binding [*out* *err*]
                                       (printf "Sass Error:\n\n%s\n"
                                               formatted)
                                       (flush))
                                     (throw exi))))
                               (finally (flush))))}])]
        #(do
           (println "Stopping watcher...")
           (hawk/stop! watcher))))
       (catch clojure.lang.ExceptionInfo exi
         (let [{ex-type :type
                :keys [formatted
                       input-path]
                :as exd} (ex-data exi)]
           (binding [*out* *err*]
             (case ex-type
               ::sass/error
               (printf "Sass Error On initial compilation:\n\n%s\n" formatted)
               ::input-not-found
               (printf "Input file not found:\n%s\n"
                       input-path)
               (printf "Unhandled Error:\n%s\n" (.getMessage exi)))))
         ;; Throw anything if it's initial comp
         (throw exi))))


(defn wrap-main!
  "Wrap a -main function (for instance, #'figwheel.main/-main) so that a watch
   starts when it starts, and terminates when it terminates."
  [main-var
   input-path
   output-path
   & add-watch-paths]
  (alter-var-root
   main-var
   (fn [mf]
     (fn [& args]
       (let [stop-fn
             (apply
              watch-compile
              input-path
              output-path
              add-watch-paths)]
         (.. Runtime
             getRuntime
             (addShutdownHook
              (Thread.
               (fn []
                 (try (stop-fn)
                      (catch java.lang.InterruptedException ie
                        (binding [*out* *err*]
                          (println "Shutdown interrupted!"))
                        (.printStackTrace ie)))))))
         (apply mf args))))))

(defn -main [input-path output-path & add-watch-paths]
  (let [p (promise)
        stop-fn (apply watch-compile
                       input-path
                       output-path
                       add-watch-paths)]
    (.. Runtime
        getRuntime
        (addShutdownHook
         (Thread. (fn []
                    (try (stop-fn)
                         (deliver p 0)
                         (catch java.lang.InterruptedException ie
                           (binding [*out* *err*]
                             (println "Shutdown interrupted!"))
                           (.printStackTrace ie)
                           (deliver p 1)))))))
    (System/exit (deref p))))
