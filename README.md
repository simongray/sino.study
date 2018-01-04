![sino·study](./resources/public/img/logo_min.svg)

This is the repository for [sino·study](https://sino.study), 
a web app designed to assist students of the Chinese language in various ways.

It is a single-page application written in Clojure and ClojureScript.
The frontend uses [Reagent](https://github.com/reagent-project/reagent) 
and [re-frame](https://github.com/Day8/re-frame).
Furthermore, it makes use of [secretary](https://github.com/gf3/secretary) 
and [Accountant](https://github.com/venantius/accountant) for frontend routing.
The backend is a [Compojure](https://github.com/weavejester/compojure) service.
Communication between the backend web service and the frontend app is
facilitated by [Transit](https://github.com/cognitect/transit-format).
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
then try clearing the browser cache. Note that some functionality will require
the backend to be running too, e.g. dictionary results.

## Compiling a frontend production build
To compile ClojureScript to JavaScript:

```
lein clean
lein cljsbuild once min
```
## Run Compojure application
To start a web server for the application, run:

````
lein ring server
````
Wait a bit, then browse to [http://localhost:3000](http://localhost:3000).
