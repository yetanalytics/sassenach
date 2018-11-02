(ns com.yetanalytics.sassenach-test
  (:require [clojure.test :refer :all]
            [com.yetanalytics.sassenach :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as cs])
  (:import [java.io File]))

(defn cleanup-fixture
  [f]
  (try (f)
       (finally
         (let [outf (io/file "target/test")]
           (when (.exists outf)
             (doseq [^File f (reverse (file-seq outf))]
               (.delete f)))))))

(use-fixtures :each cleanup-fixture)

(deftest compile-test
  (testing "Compile files from paths"
    (compile! "test_resources/valid/style.scss"
              "target/test/style.css"
              :source-map true)
    (is (= "h1 {
  color: red; }

body {
  margin: 0 0 0 0; }
/*# sourceMappingURL=style.css.map */"
           (slurp "target/test/style.css")))))
