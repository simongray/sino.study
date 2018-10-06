#!/usr/bin/env bash

# This script automates the following:
#   * compiles an uberjar
#   * builds and tags a docker image containing the uberjar
#   * pushes the docker image to the docker store

re=":tag \"v([^\"]+)"

# Needs to run a lein action at least once to make sure version.edn is built.
lein version

if [[ $(cat resources/version.edn) =~ $re ]]; then
    version=${BASH_REMATCH[1]}
    jarfile="sinostudy-standalone.jar"
    jarpath="target/sinostudy-standalone.jar"
    echo "version: ${version}";

    echo "removing old build artifacts"
    lein clean

    echo "building uberjar: ${jarfile}"
    lein uberjar

    echo "building docker image"
    docker build -t simongray/sino.study:latest -t simongray/sino.study:${version} --build-arg JARPATH=${jarpath} --build-arg JARFILE=${jarfile} .

    echo "pushing docker image"
    docker push simongray/sino.study
else
    echo "ERROR: could not determine current version"
fi
