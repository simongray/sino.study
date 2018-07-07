#!/usr/bin/env bash

# This script automates the following:
#   * compiles an uberjar
#   * builds and tags a docker image containing the uberjar
#   * pushes the docker image to the docker store

re="version: ([^,]+)"

if [[ $(lein v show) =~ $re ]]; then
    version=${BASH_REMATCH[1]}
    echo "version: ${version}";

    jarfile="sinostudy-${version}-standalone.jar"
    jarpath="target/sinostudy-${version}-standalone.jar"
    echo "building uberjar: ${jarfile}"
    lein uberjar

    echo "building docker image"
    docker build -t simongray/sino.study:latest -t simongray/sino.study:${version} --build-arg jarpath=${jarpath} --build-arg jarfile=${jarfile} .

    echo "pushing docker image"
    docker push simongray/sino.study
else
    echo "error: could not determine current version"
fi
