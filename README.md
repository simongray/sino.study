![sino·study](./resources/public/img/logo_min.svg)

This is the repository for [sino·study](http://sino.study), 
a web app designed to assist students of the Chinese language in various ways.
At the moment, it is primarily an advanced dictionary, 
but in the future it will also include functionality for grammatical analysis.

It is a single-page application written in [Clojure](https://clojure.org/) 
and [ClojureScript](https://clojurescript.org/).
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
* Running the sino.study app requires the sinostudy-data git repository to be 
located at ~/Code/sinostudy-data. Make sure that directory exists and pull from:
[sino.study-data](https://github.com/simongray/sino.study-data).
**Note: this applies to both dev and production environments.**

* The REPL starts out in the `user` ns with various other namespaces required.
The user ns also includes relevant custom functions for development.
Changes to e.g. dictionary data structures and most other backend development 
is best tested in the REPL.


## Developing with live re-loading
Typical development involves running a development web service locally,
while accessing the data from the service through a live-reloading frontend app.

### Local backend server
Start a lein nREPL for the project, then evaluate the following:

````
(start)
````

This will load the dictionary and start a production server using html-kit.
Wait a bit, then browse to [http://localhost:8080](http://localhost:8080).

The default port is `8080`, but it can be configured in `resources/config.edn`.
The system can be stopped again by evaluating:

```
(stop)
```

Pieces of state can be reloaded evaluating e.g. ```(restart #'dict)```.

### Run live-reloading frontend app
For frontend development, the fighweel experience is currently [integrated with 
Cursive using `fighweel-sidecar`](https://github.com/bhauman/lein-figwheel/wiki/Running-figwheel-in-a-Cursive-Clojure-REPL).
An IntelliJ REPL should be configured using the `Use clojure.main in normal JVM 
process` option and with `dev/src/clj/figwheel_repl.clj` set as the parameter.

As an alternative, it is also possible to simply run ```lein figwheel dev```.

Launch the REPL once configured, wait a bit, then browse to
[http://localhost:3449](http://localhost:3449).

Figwheel will automatically push CLJS changes to the browser, 
while preserving the application state. A hard page reload will reset the state.

If there are any issues getting the app to show up (e.g. blank page), 
then try clearing the browser cache. Note that most functionality will require
the development backend service to be running too.

### Running dev app on a mobile phone
If I want to test on a mobile phone I will typically run

````
ipconfig getifaddr en0
````

to get the local IP address of my Mac and then visit that address on port 3449 
(or whatever port is being used).


## Deploying to production
Currently, there are three steps to deploying a production Docker image:

1. compiling an uberjar
2. building the docker image
3. running a container from the image in production

### Compiling an uberjar for rapid deployment
This will create a standalone JAR file including the entire compiled app
(note: target JAR filename subject to change).

````
lein uberjar
````

The uberjar is a self-contained backend+frontend, although it does expect
the sino.study-datafiles repo to be present at the correct path!
To test that the uberjar was packaged correctly, run:

````
java -jar target/sinostudy-standalone.jar
````

(remember to replace $VERSION with the correct version number)

Wait a bit, then browse to [http://localhost:8080](http://localhost:8080).


### Building and deploying docker image

To build an image from the Dockerfile, run:

```` 
docker build -t simongray/sino.study:latest -t simongray/sino.study:${version} --build-arg JARPATH=${jarpath} --build-arg JARFILE=${jarfile} .
````

Note: this requires the uberjar built during the previous step as well as the
correct name and path of the jarfile.

It can then be pushed and pulled from the docker store by running e.g.

````
docker push simongray/sino.study
docker pull simongray/sino.study
````

The image can be run as a Docker container using:

```` 
# in production
docker run -v /root/Code/sinostudy-data:/root/Code/sinostudy-data -p 80:8080 simongray/sino.study:latest

# testing locally
docker run -v /Users/simongray/Code/sinostudy-data:/root/Code/sinostudy-data -p 80:8080 simongray/sino.study:latest
````

(this will tunnel the exposed `8080` port of the docker container
to the production system's port `80`)

Wait a little while, then visit [http://localhost:80](http://localhost:80)
or [http://sino.study](http://sino.study).

Use ````docker ps -a```` to list all containers and their assigned names.
Stop and remove containers using other relevant docker commands.
