![sino·study](./resources/public/img/logo_min.svg)

This is the repository for [sino·study](https://sino.study), 
a web app designed to assist students of the Chinese language in various ways.

The app is written in Clojure and ClojureScript.
The frontend uses reagent/re-frame and the backend is a compojure service.
The functionality is built around my own wrapper library for Stanford CoreNLP,
[Computerese](https://github.com/simongray/Computerese).

## Development

### Run re-frame application
```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

If there are any issues getting the app to show up (e.g. blank page), 
then try clearing the browser cache.

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
