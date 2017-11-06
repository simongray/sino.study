# sinostudy

A [re-frame](https://github.com/Day8/re-frame) application designed to ... well, that part is up to you.

## Development Mode

### Run re-frame application
```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build
To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
## Run compojure application
To start a web server for the application, run:

````
lein ring server
````
Wait a bit, then browse to [http://localhost:3000](http://localhost:3000).

