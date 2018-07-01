![sino·study](./resources/public/img/logo_min.svg)

This is the repository for [sino·study](https://sino.study), 
a web app designed to assist students of the Chinese language in various ways.

It is a single-page application written in Clojure and ClojureScript.
The frontend uses [Reagent](https://github.com/reagent-project/reagent) 
and [re-frame](https://github.com/Day8/re-frame).
Furthermore, it makes use of [secretary](https://github.com/gf3/secretary) 
and [Accountant](https://github.com/venantius/accountant) for frontend routing.
The backend is a [Compojure](https://github.com/weavejester/compojure) service
that is served by [http-kit](https://github.com/http-kit/http-kit).
Communication between the backend web service and the frontend app is
facilitated by [Transit](https://github.com/cognitect/transit-format).
The functionality is built around my own wrapper library for Stanford CoreNLP,
[Computerese](https://github.com/simongray/Computerese), as well as numerous
open-source datasets, most notably [CC-CEDICT](https://cc-cedict.org/) and
[makemeahanzi](https://github.com/skishore/makemeahanzi).

# Development

## Prerequisites
Running the sino.study app requires the sinostudy-data git repository to be 
located at ~/Code/sinostudy-data. Make sure that directory exists and pull from:
[sino.study-data](https://github.com/simongray/sino.study-data).

## Testing things out in the REPL
The REPL should start in the user ns with various relevant namespaces required.
The user ns also includes relevant functions for development.


## Compiling the frontend

### Run live-reloading frontend app
Note: this is only used during development.
```
lein clean
lein figwheel dev
```

Figwheel will automatically push CLJS changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

If there are any issues getting the app to show up (e.g. blank page), 
then try clearing the browser cache. Note that most functionality will require
the development backend service to be running too.


### Compiling a frontend production build
Note: this is a necessary step for production releases!

To compile ClojureScript to JavaScript:
eve
```
lein clean
lein cljsbuild once min
```

## Run backend Compojure web service

### Development server
To start a development web server for the application, run:

````
lein ring server
````
Wait a bit, then browse to [http://localhost:3000](http://localhost:3000).


### Production server
To start a production web server for the application, run:

````
lein repl
````

Once the REPL has loaded the user ns, evaluate the following:

````
(def stop-server (start-server))
````

This will start a production server using html-kit
(the returned function is used to stop the server again from the REPL).
Wait a bit, then browse to [http://localhost:8080](http://localhost:8080).


## Deployment

### Compiling an uberjar for rapid deployment
This will create a standalone JAR file including the entire compiled app
(note: target JAR filename subject to change).

````
lein uberjar
````

To test that the uberjar was packaged correctly, run:

````
java -jar target/sinostudy-0.1.0-SNAPSHOT-standalone.jar
````

Wait a bit, then browse to [http://localhost:8080](http://localhost:8080).
Note that the packaged production version expects to be running in production,
so queries are all sent to sino.study/query.


### Building a docker image

To build an image from the Dockerfile, run:

```` 
docker build -t simongray/sino.study .
````

Note: this requires the uberjar built during the previous step.

The image can be run as a Docker container using:

```` 
docker run -v /root/Code/sinostudy-data:/root/Code/sinostudy-data -p 80:8080 simongray/sino.study
````

Wait (more than usual), then visit [http://localhost:80](http://localhost:80).

Use ````docker ps```` to list running containers and their assigned names.
Stop the container again by running ````docker stop <name>````.
