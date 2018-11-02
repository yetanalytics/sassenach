# sassenach

A Clojure library to make Sass compilation a tiny bit easier. It wraps [sass4clj](https://github.com/Deraen/sass4clj), which I guess makes it a wrapper of a wrapper of a wrapper? In any case, I assure you that this is the hottest wrapper out there right now.

## Usage

### Watch & Compile

With sassenach on the classpath:

``` shell
$ clojure -m com.yetanalytics.sassenach resources/sass/style.scss resources/public/css/style.css
```

Keep it handy in your `~/.clojure/deps.edn`:

``` clojure
{...
 :aliases
 {...
  :watch-sass
  {:main-opts ["-m" "com.yetanalytics.sassenach"]
   :extra-deps
   {com.yetanalytics/sassenach
    {:git/url "https://github.com/yetanalytics/sassenach"
     :sha "1e0ad0c557463c86332e036837bb65df8fcabd9b"}}}
 ...}
...}
```

And run it:

``` shell
$ clojure -A:watch-sass resources/sass/style.scss resources/public/css/style.css
```

Or in your favorite project, with paths plugged in:

``` clojure
{...
 :aliases
 {...
  :watch-sass
  {:main-opts ["-m" "com.yetanalytics.sassenach"
               ;; input path
               "resources/sass/style.scss"
               ;; output path
               "resources/public/css/style.css"
               ;; ...and as many extra paths to walk as you like.
               ;; sass4clj is resource-aware, so you can use
               ;; this to watch files that cannot be inferred
               ;; from the scss input path.
               "some/other/path/to/watch"
               "another/one"]
   :extra-deps
   {com.yetanalytics/sassenach
    {:git/url "https://github.com/yetanalytics/sassenach"
     :sha "1e0ad0c557463c86332e036837bb65df8fcabd9b"}}}
 ...}
...}
```

And run it:

``` shell
$ clojure -A:watch-sass
```

Use the time you save to make beautiful things.

## License

Copyright © 2018 Yet Analytics Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
